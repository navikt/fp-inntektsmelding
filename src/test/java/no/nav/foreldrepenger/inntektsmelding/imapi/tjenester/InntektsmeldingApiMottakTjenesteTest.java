package no.nav.foreldrepenger.inntektsmelding.imapi.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.tjenester.InntektsmeldingApiMottakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.FellesMottakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Endringsårsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingApiMottakTjenesteTest {

    private static final String FNR = "12345678901";
    private static final String ORGNR = "999999999";
    private static final String AKTØR_ID = "1234567891011";

    private InntektsmeldingApiMottakTjeneste inntektsmeldingApiMottakTjeneste;

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private FellesMottakTjeneste fellesMottakTjeneste;

    @BeforeEach
    void setup() {
        inntektsmeldingApiMottakTjeneste = new InntektsmeldingApiMottakTjeneste(
            forespørselBehandlingTjeneste, inntektsmeldingTjeneste, fellesMottakTjeneste);
    }

    @Test
    void skal_returnere_feilrespons_når_forespørsel_ikke_finnes() {
        var foresporselUuid = UUID.randomUUID();
        var inputDto = lagInntektsmeldingDto(null);

        when(forespørselBehandlingTjeneste.hentForespørsel(foresporselUuid)).thenReturn(Optional.empty());

        var response = inntektsmeldingApiMottakTjeneste.mottaInntektsmelding(inputDto, foresporselUuid, FNR);

        assertThat(response.success()).isFalse();
        assertThat(response.inntektsmeldingUuid()).isNull();
        assertThat(response.melding()).contains("Finner ikke forespørsel for uuid");
        verify(fellesMottakTjeneste, never()).lagreOgJournalførInntektsmelding(any(), any());
    }

    @Test
    void skal_returnere_feilrespons_når_forespørsel_er_utgått() {
        var foresporselUuid = UUID.randomUUID();
        var forespørselDto = lagForespørselDto(foresporselUuid, null, ForespørselStatus.UTGÅTT);

        when(forespørselBehandlingTjeneste.hentForespørsel(foresporselUuid)).thenReturn(Optional.of(forespørselDto));

        var response = inntektsmeldingApiMottakTjeneste.mottaInntektsmelding(lagInntektsmeldingDto(null), foresporselUuid, FNR);

        assertThat(response.success()).isFalse();
        assertThat(response.melding()).contains("status forkastet");
        verify(fellesMottakTjeneste, never()).lagreOgJournalførInntektsmelding(any(), any());
    }

    @Test
    void skal_lagre_og_returnere_ok_når_inntektsmelding_er_ny() {
        var foresporselUuid = UUID.randomUUID();
        var imUuid = UUID.randomUUID();
        var inputDto = lagInntektsmeldingDto(null);
        var forespørselDto = lagForespørselDto(foresporselUuid, null, ForespørselStatus.UNDER_BEHANDLING);
        var lagretIm = lagInntektsmeldingDtoMedUuid(imUuid, null);

        when(forespørselBehandlingTjeneste.hentForespørsel(foresporselUuid)).thenReturn(Optional.of(forespørselDto));
        when(inntektsmeldingTjeneste.hentSisteInntektsmelding(foresporselUuid)).thenReturn(null);
        when(fellesMottakTjeneste.lagreOgJournalførInntektsmelding(any(), any())).thenReturn(lagretIm);

        var response = inntektsmeldingApiMottakTjeneste.mottaInntektsmelding(inputDto, foresporselUuid, FNR);

        assertThat(response.success()).isTrue();
        assertThat(response.inntektsmeldingUuid()).isEqualTo(imUuid);
        verify(fellesMottakTjeneste).behandlerForespørsel(forespørselDto, Optional.of(imUuid));
    }

    @Test
    void skal_avvise_semantisk_like_inntektsmeldinger() {
        var foresporselUuid = UUID.randomUUID();
        var inputDto = lagInntektsmeldingDto(null);
        var forespørselDto = lagForespørselDto(foresporselUuid, null, ForespørselStatus.UNDER_BEHANDLING);
        var tidligereLikIm = lagInntektsmeldingDto(null);

        when(forespørselBehandlingTjeneste.hentForespørsel(foresporselUuid)).thenReturn(Optional.of(forespørselDto));
        when(inntektsmeldingTjeneste.hentSisteInntektsmelding(foresporselUuid)).thenReturn(tidligereLikIm);

        var response = inntektsmeldingApiMottakTjeneste.mottaInntektsmelding(inputDto, foresporselUuid, FNR);

        assertThat(response.success()).isFalse();
        assertThat(response.melding()).contains("Ingen endring på ny inntektsmelding");
        verify(fellesMottakTjeneste, never()).lagreOgJournalførInntektsmelding(any(), any());
    }

    @Test
    void skal_lagre_og_returnere_ok_når_endring() {
        var foresporselUuid = UUID.randomUUID();
        var imUuid = UUID.randomUUID();
        var nyStartdato = LocalDate.now();
        var inputDto = lagInntektsmeldingDto(nyStartdato);
        var forespørselDto = lagForespørselDto(foresporselUuid, nyStartdato, ForespørselStatus.UNDER_BEHANDLING);
        var forrigeInnsendteIm = lagInntektsmeldingDto(null);
        var nyInnsendtIm = lagInntektsmeldingDtoMedUuid(imUuid, nyStartdato);

        when(forespørselBehandlingTjeneste.hentForespørsel(foresporselUuid)).thenReturn(Optional.of(forespørselDto));
        when(inntektsmeldingTjeneste.hentSisteInntektsmelding(foresporselUuid)).thenReturn(forrigeInnsendteIm);
        when(fellesMottakTjeneste.lagreOgJournalførInntektsmelding(any(), any())).thenReturn(nyInnsendtIm);

        var response = inntektsmeldingApiMottakTjeneste.mottaInntektsmelding(inputDto, foresporselUuid, FNR);

        assertThat(response.success()).isTrue();
        assertThat(response.inntektsmeldingUuid()).isEqualTo(imUuid);
        verify(fellesMottakTjeneste).behandlerForespørsel(forespørselDto, Optional.of(imUuid));
    }

    private static ForespørselDto lagForespørselDto(UUID uuid, LocalDate startdatoOverride, ForespørselStatus status) {
        var startdato = startdatoOverride == null ? LocalDate.of(2026, 1, 10) : startdatoOverride;
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
        return lagInntektsmeldingDtoMedUuid(null, startdatoOverride);
    }

    private static InntektsmeldingDto lagInntektsmeldingDtoMedUuid(UUID imUuid, LocalDate startdatoOverride) {
        var startdato = startdatoOverride == null ? LocalDate.of(2026, 1, 10) : startdatoOverride;
        var builder = InntektsmeldingDto.builder()
            .medAktørId(AktørId.fra(AKTØR_ID))
            .medArbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .medStartdato(startdato)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("12345678", "Kontakt Person"))
            .medInntekt(BigDecimal.valueOf(45000))
            .medMånedRefusjon(BigDecimal.valueOf(10000))
            .medOpphørsdatoRefusjon(startdato.plusDays(9))
            .medSøkteRefusjonsperioder(List.of(new InntektsmeldingDto.SøktRefusjon(startdato.plusDays(5), BigDecimal.valueOf(9000))))
            .medBortfaltNaturalytelsePerioder(List.of(
                new InntektsmeldingDto.BortfaltNaturalytelse(startdato.plusDays(2), Tid.TIDENES_ENDE, NaturalytelseType.BIL, BigDecimal.valueOf(1200))
            ))
            .medEndringAvInntektÅrsaker(List.of(
                new InntektsmeldingDto.Endringsårsaker(Endringsårsak.TARIFFENDRING, null, null, startdato.plusDays(1))
            ))
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medAvsenderSystem(new InntektsmeldingDto.AvsenderSystem("test-lps", "1.0.0"));
        if (imUuid != null) {
            builder.medInntektsmeldingUuid(imUuid);
        }
        return builder.build();
    }
}
