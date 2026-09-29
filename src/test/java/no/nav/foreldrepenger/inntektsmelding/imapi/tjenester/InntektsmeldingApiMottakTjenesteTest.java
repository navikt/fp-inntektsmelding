package no.nav.foreldrepenger.inntektsmelding.imapi.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.felles.InntektsmeldingStatusDto;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.InntektsmeldingStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.tjenester.InntektsmeldingApiMottakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.FellesMottakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektKontrollResultat;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektKontrollTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.EndringsårsakType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.konfig.Tid;

/**
 * Tester REST-kontraktshåndteringen (duplikat-sjekk, feilkoder, statusmapping) i imapi-flyten.
 * Selve A-inntekt-kontroll-logikken (nedetid/avvik/godkjent) er dekket av
 * {@link no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektKontrollTjeneste},
 * og mockes her via {@link InntektKontrollResultat}.
 */
@ExtendWith(MockitoExtension.class)
class InntektsmeldingApiMottakTjenesteTest {
    private static final String ORGNR = "999999999";
    private static final String AKTØR_ID = "1234567891011";

    private InntektsmeldingApiMottakTjeneste inntektsmeldingApiMottakTjeneste;

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private FellesMottakTjeneste fellesMottakTjeneste;
    @Mock
    private InntektKontrollTjeneste inntektKontrollTjeneste;

    @BeforeEach
    void setup() {
        inntektsmeldingApiMottakTjeneste = new InntektsmeldingApiMottakTjeneste(
            forespørselBehandlingTjeneste, inntektsmeldingTjeneste, fellesMottakTjeneste, inntektKontrollTjeneste);
    }

    @Test
    void skal_returnere_feilrespons_når_forespørsel_ikke_finnes() {
        var foresporselUuid = UUID.randomUUID();
        var inputDto = lagInntektsmeldingDto(null);

        when(forespørselBehandlingTjeneste.hentForespørselOptional(foresporselUuid)).thenReturn(Optional.empty());

        var response = inntektsmeldingApiMottakTjeneste.mottaInntektsmelding(inputDto, foresporselUuid);

        assertThat(response.success()).isFalse();
        assertThat(response.inntektsmeldingUuid()).isNull();
        assertThat(response.feilinformasjon().feilmelding()).contains("Finner ikke forespørsel for uuid");
        assertThat(response.feilinformasjon().referanseId()).isEqualTo(foresporselUuid.toString());
        verify(fellesMottakTjeneste, never()).lagreImOgOpprettJournalførTask(any(), any());
    }

    @Test
    void skal_returnere_feilrespons_når_forespørsel_er_utgått() {
        var foresporselUuid = UUID.randomUUID();
        var forespørselDto = lagForespørselDto(foresporselUuid, null, ForespørselStatus.UTGÅTT);

        when(forespørselBehandlingTjeneste.hentForespørselOptional(foresporselUuid)).thenReturn(Optional.of(forespørselDto));

        var response = inntektsmeldingApiMottakTjeneste.mottaInntektsmelding(lagInntektsmeldingDto(null), foresporselUuid);

        assertThat(response.success()).isFalse();
        assertThat(response.feilinformasjon().feilmelding()).contains("forkastet forespørsel");
        assertThat(response.feilinformasjon().referanseId()).isEqualTo(foresporselUuid.toString());
        verify(fellesMottakTjeneste, never()).lagreImOgOpprettJournalførTask(any(), any());
    }

    @Test
    void skal_avvise_inntektsmelding_når_oppgitt_inntekt_er_ulik_ainntekt() {
        var foresporselUuid = UUID.randomUUID();
        var imUuid = UUID.randomUUID();
        var inputDto = lagInntektsmeldingDtoMedUuid(imUuid, null, false, InntektsmeldingStatus.AVVIST);
        var forespørselDto = lagForespørselDto(foresporselUuid, null, ForespørselStatus.UNDER_BEHANDLING);
        var inntektsopplysninger = lagInntektsopplysninger(BigDecimal.valueOf(46000));

        when(forespørselBehandlingTjeneste.hentForespørselOptional(foresporselUuid)).thenReturn(Optional.of(forespørselDto));
        when(inntektsmeldingTjeneste.hentSisteInntektsmeldingForForespørsel(foresporselUuid)).thenReturn(null);
        when(inntektKontrollTjeneste.sjekkInntektMotAInntekt(any(), any())).thenReturn(
            new InntektKontrollResultat.UlikInntekt("Inntekt i inntektsmelding er ulik inntekt fra A-inntekt, og ingen endringsårsak er oppgitt",
                inntektsopplysninger));

        var response = inntektsmeldingApiMottakTjeneste.mottaInntektsmelding(inputDto, foresporselUuid);

        assertThat(response.success()).isFalse();
        assertThat(response.inntektsmeldingUuid()).isNull();
        assertThat(response.status()).isNull();
        assertThat(response.feilinformasjon().feilmelding()).contains("Inntekt i inntektsmelding er ulik inntekt fra A-inntekt, og ingen endringsårsak er oppgitt");
        assertThat(response.feilinformasjon().referanseId()).isEqualTo(foresporselUuid.toString());
        verify(fellesMottakTjeneste, never()).lagreImOgOpprettJournalførTask(any(), any());
    }

    @Test
    void skal_lagre_inntektsmelding_og_returnere_venter_vurdering_når_ainntekt_har_nedetid() {
        var foresporselUuid = UUID.randomUUID();
        var imUuid = UUID.randomUUID();
        var inputDto = lagInntektsmeldingDtoMedUuid(imUuid, null, false, InntektsmeldingStatus.VENTER_VURDERING);
        var forespørselDto = lagForespørselDto(foresporselUuid, null, ForespørselStatus.UNDER_BEHANDLING);
        var lagretIm = lagInntektsmeldingDtoMedUuid(imUuid, null, false, InntektsmeldingStatus.VENTER_VURDERING);

        when(forespørselBehandlingTjeneste.hentForespørselOptional(foresporselUuid)).thenReturn(Optional.of(forespørselDto));
        when(inntektsmeldingTjeneste.hentSisteInntektsmeldingForForespørsel(foresporselUuid)).thenReturn(null);
        when(inntektKontrollTjeneste.sjekkInntektMotAInntekt(any(), any())).thenReturn(
            new InntektKontrollResultat.Nedetid("Inntektskomponenten har nedetid, ..."));
        when(fellesMottakTjeneste.lagreImOgOpprettTaskForEtterkontroll(any(), any())).thenReturn(lagretIm);

        var response = inntektsmeldingApiMottakTjeneste.mottaInntektsmelding(inputDto, foresporselUuid);

        assertThat(response.success()).isTrue();
        assertThat(response.inntektsmeldingUuid()).isEqualTo(imUuid);
        assertThat(response.status()).isEqualTo(InntektsmeldingStatusDto.VENTER_VURDERING);
        assertThat(response.feilinformasjon().feilmelding()).contains("nedetid");
        assertThat(response.feilinformasjon().referanseId()).isEqualTo(foresporselUuid.toString());
        verify(fellesMottakTjeneste).lagreImOgOpprettTaskForEtterkontroll(any(), any());
        verify(fellesMottakTjeneste, never()).lagreImOgOpprettJournalførTask(any(), any());
    }

    @Test
    void skal_lagre_og_returnere_ok_når_inntektsmelding_er_ny() {
        var foresporselUuid = UUID.randomUUID();
        var imUuid = UUID.randomUUID();
        var inputDto = lagInntektsmeldingDto(null);
        var forespørselDto = lagForespørselDto(foresporselUuid, null, ForespørselStatus.UNDER_BEHANDLING);
        var lagretIm = lagInntektsmeldingDtoMedUuid(imUuid, null, true, InntektsmeldingStatus.GODKJENT);
        var inntektsopplysninger = lagInntektsopplysninger(BigDecimal.valueOf(45000));

        when(forespørselBehandlingTjeneste.hentForespørselOptional(foresporselUuid)).thenReturn(Optional.of(forespørselDto));
        when(inntektsmeldingTjeneste.hentSisteInntektsmeldingForForespørsel(foresporselUuid)).thenReturn(null);
        when(inntektKontrollTjeneste.sjekkInntektMotAInntekt(any(), any())).thenReturn(
            new InntektKontrollResultat.Godkjent(inntektsopplysninger));
        when(fellesMottakTjeneste.lagreImOgOpprettJournalførTask(any(), any())).thenReturn(lagretIm);

        var response = inntektsmeldingApiMottakTjeneste.mottaInntektsmelding(inputDto, foresporselUuid);

        assertThat(response.success()).isTrue();
        assertThat(response.inntektsmeldingUuid()).isEqualTo(imUuid);
        assertThat(response.status()).isEqualTo(InntektsmeldingStatusDto.GODKJENT);
        verify(fellesMottakTjeneste).ferdigstillOgOppdaterEksterneSystemer(forespørselDto, Optional.of(imUuid));
    }

    @Test
    void skal_avvise_semantisk_like_inntektsmeldinger() {
        var foresporselUuid = UUID.randomUUID();
        var inntektsmeldingUUid = UUID.randomUUID();
        var inputDto = lagInntektsmeldingDto(null);
        var forespørselDto = lagForespørselDto(foresporselUuid, null, ForespørselStatus.UNDER_BEHANDLING);
        var tidligereLikIm = lagInntektsmeldingDtoMedUuid(inntektsmeldingUUid, null, true, null);

        when(forespørselBehandlingTjeneste.hentForespørselOptional(foresporselUuid)).thenReturn(Optional.of(forespørselDto));
        when(inntektsmeldingTjeneste.hentSisteInntektsmeldingForForespørsel(foresporselUuid)).thenReturn(tidligereLikIm);

        var response = inntektsmeldingApiMottakTjeneste.mottaInntektsmelding(inputDto, foresporselUuid);

        assertThat(response.success()).isFalse();
        assertThat(response.feilinformasjon().feilmelding()).contains("Ingen endring på ny inntektsmelding");
        assertThat(response.feilinformasjon().referanseId()).isEqualTo(tidligereLikIm.getInntektsmeldingUuid().toString());
        verify(fellesMottakTjeneste, never()).lagreImOgOpprettJournalførTask(any(), any());
        verify(inntektKontrollTjeneste, never()).sjekkInntektMotAInntekt(any(), any());
    }

    @Test
    void skal_lagre_og_returnere_ok_når_endring() {
        var foresporselUuid = UUID.randomUUID();
        var imUuid = UUID.randomUUID();
        var nyStartdato = LocalDate.now();
        var inputDto = lagInntektsmeldingDto(nyStartdato);
        var forespørselDto = lagForespørselDto(foresporselUuid, nyStartdato, ForespørselStatus.UNDER_BEHANDLING);
        var forrigeInnsendteIm = lagInntektsmeldingDto(null);
        var nyInnsendtIm = lagInntektsmeldingDtoMedUuid(imUuid, nyStartdato, true, InntektsmeldingStatus.GODKJENT);
        var inntektsopplysninger = lagInntektsopplysninger(BigDecimal.valueOf(45000));

        when(forespørselBehandlingTjeneste.hentForespørselOptional(foresporselUuid)).thenReturn(Optional.of(forespørselDto));
        when(inntektsmeldingTjeneste.hentSisteInntektsmeldingForForespørsel(foresporselUuid)).thenReturn(forrigeInnsendteIm);
        when(inntektKontrollTjeneste.sjekkInntektMotAInntekt(any(), any())).thenReturn(
            new InntektKontrollResultat.Godkjent(inntektsopplysninger));
        when(fellesMottakTjeneste.lagreImOgOpprettJournalførTask(any(), any())).thenReturn(nyInnsendtIm);

        var response = inntektsmeldingApiMottakTjeneste.mottaInntektsmelding(inputDto, foresporselUuid);

        assertThat(response.success()).isTrue();
        assertThat(response.inntektsmeldingUuid()).isEqualTo(imUuid);
        assertThat(response.status()).isEqualTo(InntektsmeldingStatusDto.GODKJENT);
        verify(fellesMottakTjeneste).settForrigeInntektsmeldingUtdatertHvisVenterVurdering(forespørselDto);
        verify(fellesMottakTjeneste).ferdigstillOgOppdaterEksterneSystemer(forespørselDto, Optional.of(imUuid));
    }

    private static Inntektsopplysninger lagInntektsopplysninger(BigDecimal gjennomsnitt) {
        return new Inntektsopplysninger(gjennomsnitt, ORGNR, List.of());
    }

    private static ForespørselDto lagForespørselDto(UUID uuid, LocalDate startdatoOverride, ForespørselStatus status) {
        var startdato = startdatoOverride == null ? LocalDate.of(2026, Month.JANUARY, 10) : startdatoOverride;
        return ForespørselDto.builder()
            .uuid(uuid)
            .arbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .aktørId(AktørId.fra(AKTØR_ID))
            .ytelseType(Ytelsetype.FORELDREPENGER)
            .status(status)
            .forespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .førsteUttaksdato(startdato)
            .build();
    }

    private static InntektsmeldingDto lagInntektsmeldingDto(LocalDate startdatoOverride) {
        return lagInntektsmeldingDtoMedUuid(null, startdatoOverride, true, InntektsmeldingStatus.GODKJENT);
    }

    private static InntektsmeldingDto lagInntektsmeldingDtoMedUuid(UUID imUuid, LocalDate startdatoOverride, boolean skalHaEndringsårsak,
                                                                   InntektsmeldingStatus status) {
        var startdato = startdatoOverride == null ? LocalDate.of(2026, Month.JANUARY, 10) : startdatoOverride;
        var builder = InntektsmeldingDto.builder()
            .medAktørId(AktørId.fra(AKTØR_ID))
            .medArbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .medStartdato(startdato)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("12345678", "Kontakt Person"))
            .medInntekt(BigDecimal.valueOf(45000))
            .medMånedRefusjon(BigDecimal.valueOf(45000))
            .medOpphørsdatoRefusjon(startdato.plusDays(9))
            .medSøkteRefusjonsperioder(List.of(new InntektsmeldingDto.SøktRefusjon(startdato.plusDays(5), BigDecimal.valueOf(9000))))
            .medBortfaltNaturalytelsePerioder(List.of(
                new InntektsmeldingDto.BortfaltNaturalytelse(startdato.plusDays(2), Tid.TIDENES_ENDE, NaturalytelseType.BIL, BigDecimal.valueOf(1200))
            ))
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medAvsenderSystem(new InntektsmeldingDto.AvsenderSystem("test-lps", "1.0.0"))
            .medStatus(status);
        if (skalHaEndringsårsak) {
            builder.medEndringAvInntektÅrsaker(List.of(
                new InntektsmeldingDto.Endringsårsak(EndringsårsakType.TARIFFENDRING, null, null, startdato.plusDays(1))
            ));
        }
        if (imUuid != null) {
            builder.medInntektsmeldingUuid(imUuid);
        }
        return builder.build();
    }
}
