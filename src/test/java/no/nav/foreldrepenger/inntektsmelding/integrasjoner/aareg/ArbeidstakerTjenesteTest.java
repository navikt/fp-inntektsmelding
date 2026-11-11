package no.nav.foreldrepenger.inntektsmelding.integrasjoner.aareg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.pip.AltinnTilgangTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ArbeidsforholdType;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class ArbeidstakerTjenesteTest {

    private static final PersonIdent TILFELDIG_PERSON_IDENT = PersonIdent.fra("21073926618");

    @Mock
    private ArbeidsforholdTjeneste arbeidsforholdTjenesteMock;

    @Mock
    private AltinnTilgangTjeneste altinnTilgangTjenesteMock;

    private ArbeidstakerTjeneste arbeidstakerTjeneste;

    @BeforeEach
    void setUp() {
        this.arbeidstakerTjeneste = new ArbeidstakerTjeneste(this.arbeidsforholdTjenesteMock, this.altinnTilgangTjenesteMock);
    }

    @Test
    void returnerer_arbeidstakerinfo_om_dette_finnes() {
        var førsteFraværsdag = LocalDate.now();
        var ansettelsesperiode = new Arbeidsforhold.Ansettelsesperiode(LocalDate.now(), LocalDate.now().plusMonths(2));
        when(arbeidsforholdTjenesteMock.hentArbeidsforhold(any(), any())).thenReturn(
            List.of(new Arbeidsforhold("000000000", ansettelsesperiode, ArbeidsforholdType.ORDINÆRT_ARBEIDSFORHOLD))
        );
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften(any())).thenReturn(true);

        var resultat = arbeidstakerTjeneste.finnSøkersArbeidsforholdSomArbeidsgiverHarTilgangTil(TILFELDIG_PERSON_IDENT, førsteFraværsdag);
        assertThat(resultat)
            .isNotNull()
            .hasSize(1);

        var arbeidsforhold = resultat.getFirst();
        assertThat(arbeidsforhold.organisasjonsnummer()).isEqualTo("000000000");
        assertThat(arbeidsforhold.ansettelsesperiode()).isEqualTo(ansettelsesperiode);
    }

    @Test
    void verifiserer_arbeidsforhold_detaljer() {
        var førsteFraværsdag = LocalDate.now();
        var ansettelsesPeriode = new Arbeidsforhold.Ansettelsesperiode(LocalDate.now().minusYears(1), Tid.TIDENES_ENDE);
        when(arbeidsforholdTjenesteMock.hentArbeidsforhold(any(), any())).thenReturn(
            List.of(new Arbeidsforhold("00000000", ansettelsesPeriode, ArbeidsforholdType.ORDINÆRT_ARBEIDSFORHOLD)));
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften(any())).thenReturn(true);

        var resultat = arbeidstakerTjeneste.finnSøkersArbeidsforholdSomArbeidsgiverHarTilgangTil(TILFELDIG_PERSON_IDENT, førsteFraværsdag);

        assertThat(resultat).hasSize(1);
        var arbeidsforhold = resultat.getFirst();

        assertThat(arbeidsforhold.organisasjonsnummer()).isEqualTo("00000000");
        assertThat(arbeidsforhold.ansettelsesperiode()).isEqualTo(ansettelsesPeriode);
    }

    @Test
    void filtrerer_ut_arbeidsforhold_man_ikke_har_tilgang_til() {
        var førsteFraværsdag = LocalDate.now();
        var ansettelsesPeriode = new Arbeidsforhold.Ansettelsesperiode(LocalDate.now().minusYears(1), Tid.TIDENES_ENDE);
        var ansettelsesPeriode2 = new Arbeidsforhold.Ansettelsesperiode(LocalDate.now().minusYears(1), LocalDate.now().plusMonths(5));
        when(arbeidsforholdTjenesteMock.hentArbeidsforhold(any(), any())).thenReturn(
            List.of(
                new Arbeidsforhold("00000000", ansettelsesPeriode, ArbeidsforholdType.ORDINÆRT_ARBEIDSFORHOLD),
                new Arbeidsforhold("00000001", ansettelsesPeriode2, ArbeidsforholdType.ORDINÆRT_ARBEIDSFORHOLD)
            )
        );
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften("00000000")).thenReturn(false);
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften("00000001")).thenReturn(true);

        var resultat = arbeidstakerTjeneste.finnSøkersArbeidsforholdSomArbeidsgiverHarTilgangTil(TILFELDIG_PERSON_IDENT, førsteFraværsdag);

        assertThat(resultat).hasSize(1);
        var arbeidsforhold = resultat.getFirst();

        assertThat(arbeidsforhold.organisasjonsnummer()).isEqualTo("00000001");
        assertThat(arbeidsforhold.ansettelsesperiode()).isEqualTo(ansettelsesPeriode2);
    }

    @Test
    void godtar_søker_med_ordinært_arbeidsforhold_hos_arbeidsgiver() {
        var periode = new Arbeidsforhold.Ansettelsesperiode(LocalDate.now().minusYears(1), Tid.TIDENES_ENDE);
        when(arbeidsforholdTjenesteMock.hentArbeidsforhold(any(), any())).thenReturn(
            List.of(new Arbeidsforhold("000000001", periode, ArbeidsforholdType.ORDINÆRT_ARBEIDSFORHOLD)));
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften("000000001")).thenReturn(true);

        assertThatCode(() -> arbeidstakerTjeneste.sjekkAtSøkerHarArbeidsforholdHosArbeidsgiver(TILFELDIG_PERSON_IDENT, LocalDate.now(),
            Arbeidsgiver.fra("000000001"))).doesNotThrowAnyException();
    }

    @Test
    void avviser_søker_uten_arbeidsforhold_hos_arbeidsgiver() {
        var periode = new Arbeidsforhold.Ansettelsesperiode(LocalDate.now().minusYears(1), Tid.TIDENES_ENDE);
        when(arbeidsforholdTjenesteMock.hentArbeidsforhold(any(), any())).thenReturn(
            List.of(new Arbeidsforhold("000000002", periode, ArbeidsforholdType.ORDINÆRT_ARBEIDSFORHOLD)));
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften("000000002")).thenReturn(true);

        assertThatThrownBy(() -> arbeidstakerTjeneste.sjekkAtSøkerHarArbeidsforholdHosArbeidsgiver(TILFELDIG_PERSON_IDENT, LocalDate.now(),
            Arbeidsgiver.fra("000000001"))).isInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void avviser_søker_med_kun_ikke_ordinært_arbeidsforhold_hos_arbeidsgiver() {
        var periode = new Arbeidsforhold.Ansettelsesperiode(LocalDate.now().minusYears(1), Tid.TIDENES_ENDE);
        when(arbeidsforholdTjenesteMock.hentArbeidsforhold(any(), any())).thenReturn(
            List.of(new Arbeidsforhold("000000001", periode, ArbeidsforholdType.FRILANSER_OPPDRAGSTAKER_MED_MER)));

        assertThatThrownBy(() -> arbeidstakerTjeneste.sjekkAtSøkerHarArbeidsforholdHosArbeidsgiver(TILFELDIG_PERSON_IDENT, LocalDate.now(),
            Arbeidsgiver.fra("000000001"))).isInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void returnerer_organisasjoner_innsender_har_tilgang_til() {
        var organisasjoner = Set.of("000000000", "000000001", "000000002");
        var forventetListe = organisasjoner.stream().map(Arbeidsgiver::new).toList();

        when(altinnTilgangTjenesteMock.hentBedrifterArbeidsgiverHarTilgangTil()).thenReturn(organisasjoner);

        var resultat = arbeidstakerTjeneste.finnOrganisasjonerArbeidsgiverHarTilgangTil(TILFELDIG_PERSON_IDENT);

        assertThat(resultat)
            .hasSize(3)
            .isEqualTo(forventetListe);
    }
}
