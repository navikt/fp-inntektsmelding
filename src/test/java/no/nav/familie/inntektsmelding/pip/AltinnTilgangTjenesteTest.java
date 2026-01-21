package no.nav.familie.inntektsmelding.pip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.ArbeidsgiverAltinnTilgangerKlient;

@ExtendWith(MockitoExtension.class)
class AltinnTilgangTjenesteTest {

    protected static final boolean BRUK_ALTINN_TRE_RESSURS = false;
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
        when(arbeidsgiverAltinnTilgangerKlient.harTilgangTilBedriften(eq(ORG_NR), anyBoolean())).thenReturn(true);

        boolean harTilgang = altinnTilgangTjeneste.harTilgangTilBedriften(ORG_NR);

        assertTrue(harTilgang);
        verify(arbeidsgiverAltinnTilgangerKlient).harTilgangTilBedriften(ORG_NR, BRUK_ALTINN_TRE_RESSURS);
    }

    @Test
    void harTilgangTilBedriften_skal_returnere_false_når_tilgang_ikke_finnes() {
        when(arbeidsgiverAltinnTilgangerKlient.harTilgangTilBedriften(eq(ORG_NR), anyBoolean())).thenReturn(false);

        boolean harTilgang = altinnTilgangTjeneste.harTilgangTilBedriften(ORG_NR);

        assertFalse(harTilgang);
        verify(arbeidsgiverAltinnTilgangerKlient).harTilgangTilBedriften(ORG_NR, BRUK_ALTINN_TRE_RESSURS);
    }

    @Test
    void manglerTilgangTilBedriften_skal_returnere_true_når_tilgang_ikke_finnes() {
        when(arbeidsgiverAltinnTilgangerKlient.harTilgangTilBedriften(eq(ORG_NR), anyBoolean())).thenReturn(false);

        boolean manglerTilgang = altinnTilgangTjeneste.manglerTilgangTilBedriften(ORG_NR);

        assertTrue(manglerTilgang);
        verify(arbeidsgiverAltinnTilgangerKlient).harTilgangTilBedriften(ORG_NR, BRUK_ALTINN_TRE_RESSURS);
    }

    @Test
    void manglerTilgangTilBedriften_skal_returnere_false_når_tilgang_finnes() {
        when(arbeidsgiverAltinnTilgangerKlient.harTilgangTilBedriften(eq(ORG_NR), anyBoolean())).thenReturn(true);

        boolean manglerTilgang = altinnTilgangTjeneste.manglerTilgangTilBedriften(ORG_NR);

        assertFalse(manglerTilgang);
        verify(arbeidsgiverAltinnTilgangerKlient).harTilgangTilBedriften(ORG_NR, BRUK_ALTINN_TRE_RESSURS);
    }
}
