package no.nav.foreldrepenger.inntektsmelding.pip;

import static no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.AltinnRessurser.ALTINN_TO_TJENESTE;
import static no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.AltinnRessurser.ALTINN_TRE_INNTEKTSMELDING_RESSURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.ArbeidsgiverAltinnTilgangerKlient;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse;

@ExtendWith(MockitoExtension.class)
class AltinnTilgangTjenesteTest {

    protected static final String ORG_NR = "123456789";
    protected static final String ORG_NR_2 = "987654321";
    protected static final String ORG_NR_3 = "555555555";

    @Mock
    private ArbeidsgiverAltinnTilgangerKlient arbeidsgiverAltinnTilgangerKlient;

    private AltinnTilgangTjeneste altinnTilgangTjeneste;

    @BeforeEach
    void setUp() {
        altinnTilgangTjeneste = new AltinnTilgangTjeneste(arbeidsgiverAltinnTilgangerKlient);
    }

    @Nested
    @DisplayName("harTilgangTilBedriften")
    class HarTilgangTilBedriftenTest {

        @Test
        @DisplayName("Har tilgang via både Altinn 2 og Altinn 3 - returnerer true")
        void har_tilgang_via_begge() {
            var response = lagResponseMedTilgang(ORG_NR, List.of(ALTINN_TO_TJENESTE, ALTINN_TRE_INNTEKTSMELDING_RESSURS));
            when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

            assertThat(altinnTilgangTjeneste.harTilgangTilBedriften(ORG_NR)).isTrue();
        }

        @Test
        @DisplayName("Har tilgang kun via Altinn 3 - returnerer true")
        void har_tilgang_kun_via_altinn3() {
            var response = lagResponseMedTilgang(ORG_NR, List.of(ALTINN_TRE_INNTEKTSMELDING_RESSURS));
            when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

            assertThat(altinnTilgangTjeneste.harTilgangTilBedriften(ORG_NR)).isTrue();
        }

        @Test
        @DisplayName("Har tilgang kun via Altinn 2 - returnerer true")
        void har_tilgang_kun_via_altinn2() {
            var response = lagResponseMedTilgang(ORG_NR, List.of(ALTINN_TO_TJENESTE));
            when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

            assertThat(altinnTilgangTjeneste.harTilgangTilBedriften(ORG_NR)).isTrue();
        }

        @Test
        @DisplayName("Har ingen tilgang via verken Altinn 2 eller Altinn 3 - returnerer false")
        void har_ingen_tilgang() {
            var response = lagResponseMedTilgang(ORG_NR, List.of());
            when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

            assertThat(altinnTilgangTjeneste.harTilgangTilBedriften(ORG_NR)).isFalse();
        }

        @Test
        @DisplayName("Orgnr finnes ikke i response - returnerer false")
        void orgnr_finnes_ikke() {
            var response = lagTomResponse();
            when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

            assertThat(altinnTilgangTjeneste.harTilgangTilBedriften(ORG_NR)).isFalse();
        }

        @Test
        @DisplayName("Response er null - returnerer false")
        void response_er_null() {
            var response = new ArbeidsgiverAltinnTilgangerResponse(false, List.of(), null, Map.of());
            when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

            assertThat(altinnTilgangTjeneste.harTilgangTilBedriften(ORG_NR)).isFalse();
        }
    }

    @Nested
    @DisplayName("manglerTilgangTilBedriften")
    class ManglerTilgangTilBedriftenTest {

        @Test
        @DisplayName("Mangler tilgang når ingen tilgang finnes - returnerer true")
        void mangler_tilgang_når_ingen_tilgang() {
            var response = lagResponseMedTilgang(ORG_NR, List.of());
            when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

            assertThat(altinnTilgangTjeneste.manglerTilgangTilBedriften(ORG_NR)).isTrue();
        }

        @Test
        @DisplayName("Mangler ikke tilgang når tilgang finnes - returnerer false")
        void mangler_ikke_tilgang_når_tilgang_finnes() {
            var response = lagResponseMedTilgang(ORG_NR, List.of(ALTINN_TRE_INNTEKTSMELDING_RESSURS));
            when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

            assertThat(altinnTilgangTjeneste.manglerTilgangTilBedriften(ORG_NR)).isFalse();
        }
    }

    @Nested
    @DisplayName("hentBedrifterArbeidsgiverHarTilgangTil")
    class HentBedrifterArbeidsgiverHarTilgangTilTest {

        @Test
        @DisplayName("Altinn 3 gir flere organisasjoner enn Altinn 2")
        void altinn3_gir_flere_org_enn_altinn2() {
            var response = lagResponseMedTilgangTilOrgNr(
                Map.of(
                    ALTINN_TRE_INNTEKTSMELDING_RESSURS, List.of(ORG_NR, ORG_NR_2, ORG_NR_3),
                    ALTINN_TO_TJENESTE, List.of(ORG_NR)
                )
            );
            when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

            var resultat = altinnTilgangTjeneste.hentBedrifterArbeidsgiverHarTilgangTil();

            assertThat(resultat).containsExactlyInAnyOrder(ORG_NR, ORG_NR_2, ORG_NR_3);
        }

        @Test
        @DisplayName("Altinn 3 gir færre organisasjoner enn Altinn 2. Svar med union av begge")
        void altinn3_gir_faerre_org_enn_altinn2() {
            var response = lagResponseMedTilgangTilOrgNr(
                Map.of(
                    ALTINN_TRE_INNTEKTSMELDING_RESSURS, List.of(ORG_NR),
                    ALTINN_TO_TJENESTE, List.of(ORG_NR_2, ORG_NR_3)
                )
            );
            when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

            var resultat = altinnTilgangTjeneste.hentBedrifterArbeidsgiverHarTilgangTil();

            // Skal returnere union av begge - altinn2 org legges til
            assertThat(resultat).containsExactlyInAnyOrder(ORG_NR, ORG_NR_2, ORG_NR_3);
        }

        @Test
        @DisplayName("Like svar fra både Altinn 3 og Altinn 2. Gir lik svar.")
        void like_svar_fra_altinn3_og_altinn2() {
            var response = lagResponseMedTilgangTilOrgNr(
                Map.of(
                    ALTINN_TRE_INNTEKTSMELDING_RESSURS, List.of(ORG_NR, ORG_NR_2),
                    ALTINN_TO_TJENESTE, List.of(ORG_NR, ORG_NR_2)
                )
            );
            when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

            var resultat = altinnTilgangTjeneste.hentBedrifterArbeidsgiverHarTilgangTil();

            assertThat(resultat).containsExactlyInAnyOrder(ORG_NR, ORG_NR_2);
        }

        @Test
        @DisplayName("Ingen treff fra Altinn 3, men treff fra Altinn 2. Svar med Altinn 2 orgnr")
        void ingen_treff_fra_altinn3() {
            var response = lagResponseMedTilgangTilOrgNr(
                Map.of(
                    ALTINN_TO_TJENESTE, List.of(ORG_NR, ORG_NR_2)
                )
            );
            when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

            var resultat = altinnTilgangTjeneste.hentBedrifterArbeidsgiverHarTilgangTil();

            // Skal returnere altinn2 org når altinn3 er tom
            assertThat(resultat).containsExactlyInAnyOrder(ORG_NR, ORG_NR_2);
        }

        @Test
        @DisplayName("Ingen treff fra Altinn 2, men treff fra Altinn 3. Svar med Altinn 3 orgnr")
        void ingen_treff_fra_altinn2() {
            var response = lagResponseMedTilgangTilOrgNr(
                Map.of(
                    ALTINN_TRE_INNTEKTSMELDING_RESSURS, List.of(ORG_NR, ORG_NR_2)
                )
            );
            when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

            var resultat = altinnTilgangTjeneste.hentBedrifterArbeidsgiverHarTilgangTil();

            assertThat(resultat).containsExactlyInAnyOrder(ORG_NR, ORG_NR_2);
        }

        @Test
        @DisplayName("Ingen treff fra verken Altinn 2 eller Altinn 3. Gir tom liste.")
        void ingen_treff_fra_begge() {
            var response = lagResponseMedTilgangTilOrgNr(Map.of());
            when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

            var resultat = altinnTilgangTjeneste.hentBedrifterArbeidsgiverHarTilgangTil();

            assertThat(resultat).isEmpty();
        }
    }

    private ArbeidsgiverAltinnTilgangerResponse lagResponseMedTilgang(String orgNr, List<String> tilganger) {
        return new ArbeidsgiverAltinnTilgangerResponse(
            false,
            List.of(),
            Map.of(orgNr, tilganger),
            Map.of()
        );
    }

    private ArbeidsgiverAltinnTilgangerResponse lagResponseMedTilgangTilOrgNr(Map<String, List<String>> tilgangTilOrgNr) {
        return new ArbeidsgiverAltinnTilgangerResponse(
            false,
            List.of(),
            Map.of(),
            tilgangTilOrgNr
        );
    }

    private ArbeidsgiverAltinnTilgangerResponse lagTomResponse() {
        return new ArbeidsgiverAltinnTilgangerResponse(
            false,
            List.of(),
            Map.of(),
            Map.of()
        );
    }
}
