package no.nav.foreldrepenger.inntektsmelding.imapi.rest.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Endringsårsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingKontraktMapperTest {

    private static final String ORGNR = "999999999";
    private static final String AKTØR_ID = "1234567891011";
    private static final String FNR = "12345678901";
    private static final LocalDate STARTDATO = LocalDate.of(2026, 1, 10);

    @Mock
    private ForespørselTjeneste forespørselTjeneste;
    @Mock
    private PersonTjeneste personTjeneste;

    private InntektsmeldingKontraktMapper mapper;

    @BeforeEach
    void setup() {
        mapper = new InntektsmeldingKontraktMapper(forespørselTjeneste, personTjeneste);
    }

    @Test
    void skal_mappe_grunnleggende_felter() {
        var imUuid = UUID.randomUUID();
        var forespørselUuid = UUID.randomUUID();
        var inntektsmelding = lagInntektsmeldingDto(imUuid, STARTDATO, Kildesystem.FPSAK);

        when(forespørselTjeneste.finnForespørsler(any(), any(), eq(ORGNR)))
            .thenReturn(List.of(lagForespørselDto(forespørselUuid, STARTDATO)));
        when(personTjeneste.finnPersonIdentForAktørId(any()))
            .thenReturn(new PersonIdent(FNR));

        var resultat = mapper.mapTilKontrakt(inntektsmelding);

        assertThat(resultat.inntektsmeldingUuid()).isEqualTo(imUuid);
        assertThat(resultat.forespørselUuid()).isEqualTo(forespørselUuid);
        assertThat(resultat.fnr().fnr()).isEqualTo(FNR);
        assertThat(resultat.ytelseType().name()).isEqualTo("FORELDREPENGER");
        assertThat(resultat.arbeidsgiver().orgnr()).isEqualTo(ORGNR);
        assertThat(resultat.kontaktperson().navn()).isEqualTo("Kontakt Person");
        assertThat(resultat.kontaktperson().telefonnummer()).isEqualTo("12345678");
        assertThat(resultat.startdato()).isEqualTo(STARTDATO);
        assertThat(resultat.inntekt()).isEqualByComparingTo(BigDecimal.valueOf(45000));
    }

    @Test
    void skal_mappe_refusjonsfelter() {
        var imUuid = UUID.randomUUID();
        var inntektsmelding = lagInntektsmeldingDto(imUuid, STARTDATO, Kildesystem.FPSAK);

        when(forespørselTjeneste.finnForespørsler(any(), any(), eq(ORGNR)))
            .thenReturn(List.of(lagForespørselDto(UUID.randomUUID(), STARTDATO)));
        when(personTjeneste.finnPersonIdentForAktørId(any()))
            .thenReturn(new PersonIdent(FNR));

        var resultat = mapper.mapTilKontrakt(inntektsmelding);

        assertThat(resultat.refusjonPrMnd()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(resultat.opphørsdatoRefusjon()).isEqualTo(STARTDATO.plusDays(9));
        assertThat(resultat.refusjonsendringer()).hasSize(1);
        assertThat(resultat.refusjonsendringer().getFirst().fom()).isEqualTo(STARTDATO.plusDays(5));
        assertThat(resultat.refusjonsendringer().getFirst().beløp()).isEqualByComparingTo(BigDecimal.valueOf(9000));
    }

    @Test
    void skal_mappe_bortfalt_naturalytelse() {
        var imUuid = UUID.randomUUID();
        var inntektsmelding = lagInntektsmeldingDto(imUuid, STARTDATO, Kildesystem.FPSAK);

        when(forespørselTjeneste.finnForespørsler(any(), any(), eq(ORGNR)))
            .thenReturn(List.of(lagForespørselDto(UUID.randomUUID(), STARTDATO)));
        when(personTjeneste.finnPersonIdentForAktørId(any()))
            .thenReturn(new PersonIdent(FNR));

        var resultat = mapper.mapTilKontrakt(inntektsmelding);

        assertThat(resultat.bortfaltNaturalytelsePerioder()).hasSize(1);
        var bortfalt = resultat.bortfaltNaturalytelsePerioder().getFirst();
        assertThat(bortfalt.fom()).isEqualTo(STARTDATO.plusDays(2));
        assertThat(bortfalt.naturalytelsetype().name()).isEqualTo("BIL");
        assertThat(bortfalt.beløp()).isEqualByComparingTo(BigDecimal.valueOf(1200));
    }

    @Test
    void skal_mappe_endringsårsaker() {
        var imUuid = UUID.randomUUID();
        var inntektsmelding = lagInntektsmeldingDto(imUuid, STARTDATO, Kildesystem.FPSAK);

        when(forespørselTjeneste.finnForespørsler(any(), any(), eq(ORGNR)))
            .thenReturn(List.of(lagForespørselDto(UUID.randomUUID(), STARTDATO)));
        when(personTjeneste.finnPersonIdentForAktørId(any()))
            .thenReturn(new PersonIdent(FNR));

        var resultat = mapper.mapTilKontrakt(inntektsmelding);

        assertThat(resultat.endringAvInntektÅrsaker()).hasSize(1);
        var endring = resultat.endringAvInntektÅrsaker().getFirst();
        assertThat(endring.årsak().name()).isEqualTo("TARIFFENDRING");
        assertThat(endring.bleKjentFom()).isEqualTo(STARTDATO.plusDays(1));
    }

    @Test
    void skal_mappe_avsendersystem() {
        var imUuid = UUID.randomUUID();
        var inntektsmelding = lagInntektsmeldingDto(imUuid, STARTDATO, Kildesystem.FPSAK);

        when(forespørselTjeneste.finnForespørsler(any(), any(), eq(ORGNR)))
            .thenReturn(List.of(lagForespørselDto(UUID.randomUUID(), STARTDATO)));
        when(personTjeneste.finnPersonIdentForAktørId(any()))
            .thenReturn(new PersonIdent(FNR));

        var resultat = mapper.mapTilKontrakt(inntektsmelding);

        assertThat(resultat.avsenderSystem()).isNotNull();
        assertThat(resultat.avsenderSystem().systemNavn()).isEqualTo("test-lps");
        assertThat(resultat.avsenderSystem().systemVersjon()).isEqualTo("1.0.0");
    }

    @Test
    void skal_mappe_avsendersystem_null_når_ikke_satt() {
        var imUuid = UUID.randomUUID();
        var inntektsmelding = lagInntektsmeldingDtoUtenAvsenderSystem(imUuid);

        when(forespørselTjeneste.finnForespørsler(any(), any(), eq(ORGNR)))
            .thenReturn(List.of(lagForespørselDto(UUID.randomUUID(), STARTDATO)));
        when(personTjeneste.finnPersonIdentForAktørId(any()))
            .thenReturn(new PersonIdent(FNR));

        var resultat = mapper.mapTilKontrakt(inntektsmelding);

        assertThat(resultat.avsenderSystem()).isNull();
    }

    private static ForespørselDto lagForespørselDto(UUID uuid, LocalDate startdato) {
        return ForespørselDto.builder()
            .uuid(uuid)
            .arbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .aktørId(AktørId.fra(AKTØR_ID))
            .ytelseType(Ytelsetype.FORELDREPENGER)
            .status(ForespørselStatus.UNDER_BEHANDLING)
            .forespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .førsteUttaksdato(startdato)
            .build();
    }

    private static InntektsmeldingDto lagInntektsmeldingDto(UUID imUuid, LocalDate startdato, Kildesystem kildesystem) {
        return InntektsmeldingDto.builder()
            .medInntektsmeldingUuid(imUuid)
            .medAktørId(AktørId.fra(AKTØR_ID))
            .medArbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .medStartdato(startdato)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("12345678", "Kontakt Person"))
            .medInntekt(BigDecimal.valueOf(45000))
            .medMånedRefusjon(BigDecimal.valueOf(10000))
            .medOpphørsdatoRefusjon(startdato.plusDays(9))
            .medInnsendtTidspunkt(LocalDateTime.of(2026, 1, 10, 12, 0))
            .medSøkteRefusjonsperioder(List.of(new InntektsmeldingDto.SøktRefusjon(startdato.plusDays(5), BigDecimal.valueOf(9000))))
            .medBortfaltNaturalytelsePerioder(List.of(
                new InntektsmeldingDto.BortfaltNaturalytelse(startdato.plusDays(2), Tid.TIDENES_ENDE, NaturalytelseType.BIL, BigDecimal.valueOf(1200))
            ))
            .medEndringAvInntektÅrsaker(List.of(
                new InntektsmeldingDto.Endringsårsaker(Endringsårsak.TARIFFENDRING, null, null, startdato.plusDays(1))
            ))
            .medKildesystem(kildesystem)
            .medAvsenderSystem(new InntektsmeldingDto.AvsenderSystem("test-lps", "1.0.0"))
            .build();
    }

    private static InntektsmeldingDto lagInntektsmeldingDtoUtenAvsenderSystem(UUID imUuid) {
        return InntektsmeldingDto.builder()
            .medInntektsmeldingUuid(imUuid)
            .medAktørId(AktørId.fra(AKTØR_ID))
            .medArbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .medStartdato(STARTDATO)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("12345678", "Kontakt Person"))
            .medInntekt(BigDecimal.valueOf(45000))
            .medInnsendtTidspunkt(LocalDateTime.of(2026, 1, 10, 12, 0))
            .medKildesystem(Kildesystem.FPSAK)
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(List.of())
            .medEndringAvInntektÅrsaker(List.of())
            .build();
    }
}
