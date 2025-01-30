package no.nav.familie.inntektsmelding.integrasjoner.aareg;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.pip.AltinnTilgangTjeneste;

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
        when(arbeidsforholdTjenesteMock.hentArbeidsforhold(any(), any())).thenReturn(
            List.of(new Arbeidsforhold("000000000", "111111111"))
        );
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften(any())).thenReturn(true);

        var resultat = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(TILFELDIG_PERSON_IDENT, førsteFraværsdag);
        assertThat(resultat).isNotNull();
        assertThat(resultat.size()).isEqualTo(1);

        var arbeidsforhold = resultat.getFirst();
        assertThat(arbeidsforhold.organisasjonsnummer()).isEqualTo("000000000");
        assertThat(arbeidsforhold.arbeidsforholdId()).isEqualTo("111111111");
    }

    @Test
    void verifiserer_arbeidsforhold_detaljer() {
        var førsteFraværsdag = LocalDate.now();
        when(arbeidsforholdTjenesteMock.hentArbeidsforhold(any(), any())).thenReturn(
            List.of(new Arbeidsforhold("00000000", "123456789")));
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften(any())).thenReturn(true);

        var resultat = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(TILFELDIG_PERSON_IDENT, førsteFraværsdag);

        assertThat(resultat.size()).isEqualTo(1);
        var arbeidsforhold = resultat.get(0);

        assertThat(arbeidsforhold.organisasjonsnummer()).isEqualTo("00000000");
        assertThat(arbeidsforhold.arbeidsforholdId()).isEqualTo("123456789");
    }

    @Test
    void filtrerer_ut_arbeidsforhold_man_ikke_har_tilgang_til() {
        var førsteFraværsdag = LocalDate.now();
        when(arbeidsforholdTjenesteMock.hentArbeidsforhold(any(), any())).thenReturn(
            List.of(
                new Arbeidsforhold("00000000", "123456789"),
                new Arbeidsforhold("00000001", "123456789")
            )
        );
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften("00000000")).thenReturn(false);
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften("00000001")).thenReturn(true);

        var resultat = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(TILFELDIG_PERSON_IDENT, førsteFraværsdag);

        assertThat(resultat.size()).isEqualTo(1);
        var arbeidsforhold = resultat.getFirst();

        assertThat(arbeidsforhold.organisasjonsnummer()).isEqualTo("00000001");
        assertThat(arbeidsforhold.arbeidsforholdId()).isEqualTo("123456789");
    }
}
