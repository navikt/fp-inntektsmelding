package no.nav.foreldrepenger.inntektsmelding.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselRepository;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.InntektsmeldingEntitet;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.InntektsmeldingRepository;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.InntektsmeldingStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;

/**
 * Dekker at «duplikat»-sjekken (via {@link InntektsmeldingTjeneste#hentSisteInntektsmeldingForForespørsel})
 * ikke lenger hindrer arbeidsgiver i å sende inn på nytt når den forrige inntektsmeldingen er
 * AVVIST eller UTDATERT, mens innsending fortsatt anses som duplikat når forrige er VENTER_VURDERING.
 */
@ExtendWith(MockitoExtension.class)
class InntektsmeldingTjenesteTest {

    private static final String ORGNR = "999999999";
    private static final String AKTØR_ID = "9999999999999";
    private static final LocalDate STARTDATO = LocalDate.of(2026, 1, 10);

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private InntektsmeldingRepository inntektsmeldingRepository;
    @Mock
    private ForespørselRepository forespørselRepository;

    private UUID forespørselUuid;

    @BeforeEach
    void setup() {
        inntektsmeldingTjeneste = new InntektsmeldingTjeneste(forespørselBehandlingTjeneste, inntektsmeldingRepository, forespørselRepository);
        forespørselUuid = UUID.randomUUID();
        var forespørselDto = lagForespørselDto(forespørselUuid);
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(forespørselDto);
    }

    @Test
    void skal_ikke_returnere_avvist_inntektsmelding_som_siste_slik_at_ny_innsending_ikke_avvises_som_duplikat() {
        var avvistIm = lagInntektsmelding(InntektsmeldingStatus.AVVIST);
        when(inntektsmeldingRepository.hentInntektsmeldingerSortertNyesteFørst(any(), any(), any(), any())).thenReturn(List.of(avvistIm));

        var siste = inntektsmeldingTjeneste.hentSisteInntektsmeldingForForespørsel(forespørselUuid);

        assertThat(siste).isNull();
    }

    @Test
    void skal_ikke_returnere_utdatert_inntektsmelding_som_siste_slik_at_ny_innsending_ikke_avvises_som_duplikat() {
        var utdatertIm = lagInntektsmelding(InntektsmeldingStatus.UTDATERT);
        when(inntektsmeldingRepository.hentInntektsmeldingerSortertNyesteFørst(any(), any(), any(), any())).thenReturn(List.of(utdatertIm));

        var siste = inntektsmeldingTjeneste.hentSisteInntektsmeldingForForespørsel(forespørselUuid);

        assertThat(siste).isNull();
    }

    @Test
    void skal_fortsatt_returnere_inntektsmelding_som_venter_vurdering_som_siste_slik_at_ny_lik_innsending_fortsatt_er_duplikat() {
        var venterVurderingIm = lagInntektsmelding(InntektsmeldingStatus.VENTER_VURDERING);
        when(inntektsmeldingRepository.hentInntektsmeldingerSortertNyesteFørst(any(), any(), any(), any())).thenReturn(List.of(venterVurderingIm));

        var siste = inntektsmeldingTjeneste.hentSisteInntektsmeldingForForespørsel(forespørselUuid);

        assertThat(siste).isNotNull();
        assertThat(siste.getStatus()).isEqualTo(InntektsmeldingStatus.VENTER_VURDERING);
    }

    @Test
    void skal_hoppe_over_avvist_og_finne_godkjent_som_siste_gyldige_inntektsmelding() {
        var avvistIm = lagInntektsmelding(InntektsmeldingStatus.AVVIST);
        var godkjentIm = lagInntektsmelding(InntektsmeldingStatus.GODKJENT);
        // Repositoriet returnerer nyeste først - avvist ligger foran den eldre, godkjente
        when(inntektsmeldingRepository.hentInntektsmeldingerSortertNyesteFørst(any(), any(), any(), any())).thenReturn(List.of(avvistIm, godkjentIm));

        var siste = inntektsmeldingTjeneste.hentSisteInntektsmeldingForForespørsel(forespørselUuid);

        assertThat(siste).isNotNull();
        assertThat(siste.getStatus()).isEqualTo(InntektsmeldingStatus.GODKJENT);
    }

    @Test
    void hentAlleInntektsmeldinger_skal_ikke_filtrere_bort_avvist_eller_utdatert() {
        var avvistIm = lagInntektsmelding(InntektsmeldingStatus.AVVIST);
        var utdatertIm = lagInntektsmelding(InntektsmeldingStatus.UTDATERT);
        when(inntektsmeldingRepository.hentInntektsmeldingerSortertNyesteFørst(any(), any(), any(), any())).thenReturn(List.of(avvistIm, utdatertIm));

        var alle = inntektsmeldingTjeneste.hentAlleInntektsmeldinger(forespørselUuid);

        assertThat(alle).hasSize(2);
    }

    private static ForespørselDto lagForespørselDto(UUID uuid) {
        return ForespørselDto.builder()
            .uuid(uuid)
            .arbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .aktørId(AktørId.fra(AKTØR_ID))
            .ytelseType(Ytelsetype.FORELDREPENGER)
            .forespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .førsteUttaksdato(STARTDATO)
            .build();
    }

    private static InntektsmeldingEntitet lagInntektsmelding(InntektsmeldingStatus status) {
        var forespørselEntitet = new ForespørselEntitet(ORGNR, STARTDATO, new AktørIdEntitet(AKTØR_ID), Ytelsetype.FORELDREPENGER, "SAK-1", STARTDATO,
            ForespørselType.BESTILT_AV_FAGSYSTEM);
        return InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet(AKTØR_ID))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medArbeidsgiverIdent(ORGNR)
            .medStartDato(STARTDATO)
            .medMånedInntekt(BigDecimal.valueOf(45000))
            .medForespørsel(forespørselEntitet)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medStatus(status)
            .build();
    }
}
