package no.nav.familie.inntektsmelding.pip;

import static no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnRessurser.ALTINN_TO_TJENESTE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.ArbeidsgiverAltinnTilgangerKlient;
import no.nav.familie.inntektsmelding.integrasjoner.altinn.ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse;

@ExtendWith(MockitoExtension.class)
class AltinnTilgangTjenesteTest {

    protected static final String ORG_NR = "123456789";

    @Mock
    private ArbeidsgiverAltinnTilgangerKlient arbeidsgiverAltinnTilgangerKlient;

    private AltinnTilgangTjeneste altinnTilgangTjeneste;

    @BeforeEach
    void setUp() {
        altinnTilgangTjeneste = new AltinnTilgangTjeneste(arbeidsgiverAltinnTilgangerKlient);
    }

    @Test
    void harTilgangTilBedriften_skal_returnere_true_når_tilgang_finnes() {
        var response = lagResponseMedTilgang(ORG_NR, List.of(ALTINN_TO_TJENESTE));
        when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

        boolean harTilgang = altinnTilgangTjeneste.harTilgangTilBedriften(ORG_NR);

        assertTrue(harTilgang);
        verify(arbeidsgiverAltinnTilgangerKlient).hentTilganger();
    }

    @Test
    void harTilgangTilBedriften_skal_returnere_false_når_tilgang_ikke_finnes() {
        var response = lagResponseMedTilgang(ORG_NR, List.of());
        when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

        boolean harTilgang = altinnTilgangTjeneste.harTilgangTilBedriften(ORG_NR);

        assertFalse(harTilgang);
        verify(arbeidsgiverAltinnTilgangerKlient).hentTilganger();
    }

    @Test
    void harTilgangTilBedriften_skal_returnere_false_når_orgnr_ikke_finnes() {
        var response = lagTomResponse();
        when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

        boolean harTilgang = altinnTilgangTjeneste.harTilgangTilBedriften(ORG_NR);

        assertFalse(harTilgang);
        verify(arbeidsgiverAltinnTilgangerKlient).hentTilganger();
    }

    @Test
    void manglerTilgangTilBedriften_skal_returnere_true_når_tilgang_ikke_finnes() {
        var response = lagResponseMedTilgang(ORG_NR, List.of());
        when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

        boolean manglerTilgang = altinnTilgangTjeneste.manglerTilgangTilBedriften(ORG_NR);

        assertTrue(manglerTilgang);
        verify(arbeidsgiverAltinnTilgangerKlient).hentTilganger();
    }

    @Test
    void manglerTilgangTilBedriften_skal_returnere_false_når_tilgang_finnes() {
        var response = lagResponseMedTilgang(ORG_NR, List.of(ALTINN_TO_TJENESTE));
        when(arbeidsgiverAltinnTilgangerKlient.hentTilganger()).thenReturn(response);

        boolean manglerTilgang = altinnTilgangTjeneste.manglerTilgangTilBedriften(ORG_NR);

        assertFalse(manglerTilgang);
        verify(arbeidsgiverAltinnTilgangerKlient).hentTilganger();
    }

    private ArbeidsgiverAltinnTilgangerResponse lagResponseMedTilgang(String orgNr, List<String> tilganger) {
        return new ArbeidsgiverAltinnTilgangerResponse(
            false,
            List.of(),
            Map.of(orgNr, tilganger),
            Map.of()
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
