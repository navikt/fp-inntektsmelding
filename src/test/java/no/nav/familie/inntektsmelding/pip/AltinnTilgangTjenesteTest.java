package no.nav.familie.inntektsmelding.pip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnRettigheterProxyKlient;

@ExtendWith(MockitoExtension.class)
class AltinnTilgangTjenesteTest {

  @Mock
  private AltinnRettigheterProxyKlient altinnKlient;

  private AltinnTilgangTjeneste altinnTilgangTjeneste;

  @BeforeEach
  void setUp() {
    altinnTilgangTjeneste = new AltinnTilgangTjeneste(altinnKlient);
  }

  @Test
  void harTilgangTilBedriften_skal_returnere_true_når_tilgang_finnes() {
    String orgNr = "123456789";
    when(altinnKlient.harTilgangTilBedriften(orgNr)).thenReturn(true);

    boolean harTilgang = altinnTilgangTjeneste.harTilgangTilBedriften(orgNr);

    assertTrue(harTilgang);
    verify(altinnKlient).harTilgangTilBedriften(orgNr);
  }

  @Test
  void harTilgangTilBedriften_skal_returnere_false_når_tilgang_ikke_finnes() {
    String orgNr = "123456789";
    when(altinnKlient.harTilgangTilBedriften(orgNr)).thenReturn(false);

    boolean harTilgang = altinnTilgangTjeneste.harTilgangTilBedriften(orgNr);

    assertFalse(harTilgang);
    verify(altinnKlient).harTilgangTilBedriften(orgNr);
  }

  @Test
  void manglerTilgangTilBedriften_skal_returnere_true_når_tilgang_ikke_finnes() {
    String orgNr = "123456789";
    when(altinnKlient.harTilgangTilBedriften(orgNr)).thenReturn(false);

    boolean manglerTilgang = altinnTilgangTjeneste.manglerTilgangTilBedriften(orgNr);

    assertTrue(manglerTilgang);
    verify(altinnKlient).harTilgangTilBedriften(orgNr);
  }

  @Test
  void manglerTilgangTilBedriften_skal_returnere_false_når_tilgang_finnes() {
    String orgNr = "123456789";
    when(altinnKlient.harTilgangTilBedriften(orgNr)).thenReturn(true);

    boolean manglerTilgang = altinnTilgangTjeneste.manglerTilgangTilBedriften(orgNr);

    assertFalse(manglerTilgang);
    verify(altinnKlient).harTilgangTilBedriften(orgNr);
  }
}
