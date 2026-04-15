package no.nav.foreldrepenger.inntektsmelding.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.task.SendTilJoarkTask;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class FellesMottakTjenesteTest {

    private static final String ORGNR = "999999999";
    private static final String AKTØR_ID = "1234567891011";
    private static final LocalDate STARTDATO = LocalDate.of(2026, 1, 10);

    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    private FellesMottakTjeneste fellesMottakTjeneste;

    @BeforeEach
    void setUp() {
        fellesMottakTjeneste = new FellesMottakTjeneste(
            inntektsmeldingTjeneste,
            prosessTaskTjeneste,
            forespørselBehandlingTjeneste
        );
    }

    @Test
    void skal_lagre_og_journalføre_inntektsmelding() {
        // Arrange
        var imId = 123L;
        var forespørselUuid = UUID.randomUUID();
        var inntektsmeldingDto = lagInntektsmeldingDto();
        var forespørselDto = lagForespørselDto(forespørselUuid, null);
        var lagretInntektsmelding = lagInntektsmeldingDtoMedUuid(UUID.randomUUID());

        when(inntektsmeldingTjeneste.lagreInntektsmelding(any())).thenReturn(imId);
        when(inntektsmeldingTjeneste.hentInntektsmelding(imId)).thenReturn(lagretInntektsmelding);

        // Act
        var resultat = fellesMottakTjeneste.lagreOgJournalførInntektsmelding(inntektsmeldingDto, forespørselDto);

        // Assert
        assertThat(resultat).isNotNull();
        assertThat(resultat.getInntektsmeldingUuid()).isNotNull();
        verify(inntektsmeldingTjeneste).lagreInntektsmelding(inntektsmeldingDto);
        verify(inntektsmeldingTjeneste).hentInntektsmelding(imId);

        // Verifiser at task ble opprettet
        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(taskCaptor.capture());
        var task = taskCaptor.getValue();
        assertThat(task.getPropertyValue(SendTilJoarkTask.KEY_FORESPOERSEL_TYPE))
            .isEqualTo(forespørselDto.forespørselType().toString());
    }

    @Test
    void skal_sette_saksnummer_på_task_når_fagsystemsaksnummer_finnes() {
        // Arrange
        var imId = 123L;
        var forespørselUuid = UUID.randomUUID();
        var saksnummer = Saksnummer.fra("SAK_123456");
        var inntektsmeldingDto = lagInntektsmeldingDto();
        var forespørselDto = ForespørselDto.builder()
            .uuid(forespørselUuid)
            .arbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .aktørId(AktørId.fra(AKTØR_ID))
            .ytelseType(Ytelsetype.FORELDREPENGER)
            .status(ForespørselStatus.UNDER_BEHANDLING)
            .forespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .førsteUttaksdato(STARTDATO)
            .fagsystemSaksnummer(saksnummer)
            .build();
        var lagretInntektsmelding = lagInntektsmeldingDtoMedUuid(UUID.randomUUID());

        when(inntektsmeldingTjeneste.lagreInntektsmelding(any())).thenReturn(imId);
        when(inntektsmeldingTjeneste.hentInntektsmelding(imId)).thenReturn(lagretInntektsmelding);

        // Act
        fellesMottakTjeneste.lagreOgJournalførInntektsmelding(inntektsmeldingDto, forespørselDto);

        // Assert
        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(taskCaptor.capture());
        var task = taskCaptor.getValue();
        assertThat(task.getSaksnummer()).isEqualTo(saksnummer.saksnummer());
    }

    @Test
    void skal_ikke_sette_saksnummer_på_task_når_fagsystemsaksnummer_ikke_finnes() {
        // Arrange
        var imId = 123L;
        var forespørselUuid = UUID.randomUUID();
        var inntektsmeldingDto = lagInntektsmeldingDto();
        var forespørselDto = lagForespørselDto(forespørselUuid, null);
        var lagretInntektsmelding = lagInntektsmeldingDtoMedUuid(UUID.randomUUID());

        when(inntektsmeldingTjeneste.lagreInntektsmelding(any())).thenReturn(imId);
        when(inntektsmeldingTjeneste.hentInntektsmelding(imId)).thenReturn(lagretInntektsmelding);

        // Act
        fellesMottakTjeneste.lagreOgJournalførInntektsmelding(inntektsmeldingDto, forespørselDto);

        // Assert
        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(taskCaptor.capture());
        var task = taskCaptor.getValue();
        assertThat(task.getSaksnummer()).isNull();
    }

    @Test
    void skal_ferdigstille_forespørsel_når_status_ikke_er_ferdig() {
        // Arrange
        var forespørselUuid = UUID.randomUUID();
        var imUuid = UUID.randomUUID();
        var forespørselDto = lagForespørselDto(forespørselUuid, ForespørselStatus.UNDER_BEHANDLING);
        var ferdigstiltForespørselDto = lagForespørselDto(forespørselUuid, ForespørselStatus.FERDIG);

        when(forespørselBehandlingTjeneste.ferdigstillForespørsel(
            forespørselDto.uuid(),
            forespørselDto.aktørId(),
            forespørselDto.arbeidsgiver(),
            forespørselDto.førsteUttaksdato(),
            LukkeÅrsak.ORDINÆR_INNSENDING,
            Optional.of(imUuid)
        )).thenReturn(ferdigstiltForespørselDto);

        // Act
        fellesMottakTjeneste.behandlerForespørsel(forespørselDto, Optional.of(imUuid));

        // Assert
        verify(forespørselBehandlingTjeneste).ferdigstillForespørsel(
            forespørselDto.uuid(),
            forespørselDto.aktørId(),
            forespørselDto.arbeidsgiver(),
            forespørselDto.førsteUttaksdato(),
            LukkeÅrsak.ORDINÆR_INNSENDING,
            Optional.of(imUuid)
        );
    }

    @Test
    void skal_ikke_ferdigstille_forespørsel_når_status_allerede_er_ferdig() {
        // Arrange
        var forespørselUuid = UUID.randomUUID();
        var imUuid = UUID.randomUUID();
        var forespørselDto = lagForespørselDto(forespørselUuid, ForespørselStatus.FERDIG);

        // Act
        fellesMottakTjeneste.behandlerForespørsel(forespørselDto, Optional.of(imUuid));

        // Assert
        verify(forespørselBehandlingTjeneste).oppdaterPortalerMedEndretInntektsmelding(
            forespørselDto,
            Optional.of(imUuid),
            forespørselDto.arbeidsgiver()
        );
    }


    private static ForespørselDto lagForespørselDto(UUID uuid, ForespørselStatus status) {
        return ForespørselDto.builder()
            .uuid(uuid)
            .arbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .aktørId(AktørId.fra(AKTØR_ID))
            .ytelseType(Ytelsetype.FORELDREPENGER)
            .status(status)
            .forespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .førsteUttaksdato(STARTDATO)
            .build();
    }

    private static InntektsmeldingDto lagInntektsmeldingDto() {
        return lagInntektsmeldingDtoMedUuid(null);
    }

    private static InntektsmeldingDto lagInntektsmeldingDtoMedUuid(UUID imUuid) {
        var builder = InntektsmeldingDto.builder()
            .medAktørId(AktørId.fra(AKTØR_ID))
            .medArbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .medStartdato(STARTDATO)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("12345678", "Kontakt Person"))
            .medInntekt(BigDecimal.valueOf(45000))
            .medMånedRefusjon(BigDecimal.valueOf(10000))
            .medOpphørsdatoRefusjon(STARTDATO.plusDays(9))
            .medSøkteRefusjonsperioder(List.of(
                new InntektsmeldingDto.SøktRefusjon(STARTDATO.plusDays(5), BigDecimal.valueOf(9000))
            ))
            .medBortfaltNaturalytelsePerioder(List.of(
                new InntektsmeldingDto.BortfaltNaturalytelse(
                    STARTDATO.plusDays(2),
                    Tid.TIDENES_ENDE,
                    NaturalytelseType.BIL,
                    BigDecimal.valueOf(1200)
                )
            ))
            .medEndringAvInntektÅrsaker(List.of());

        if (imUuid != null) {
            builder.medInntektsmeldingUuid(imUuid);
        }

        return builder.build();
    }
}

