package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import static no.nav.familie.inntektsmelding.integrasjoner.altinn.ArbeidsgiverAltinnTilgangerKlient.ALTINN_TO_TJENESTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.sikkerhet.kontekst.BasisKontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ExtendWith(MockitoExtension.class)
class ArbeidsgiverAltinnTilgangerKlientTest {

    protected static final String NAV_TEST_RESSURS = "nav_test_ressurs";
    protected static final String TEST_ORGNR = "999999999";
    @Mock
    RestClient klient;

    @BeforeEach
    void setUp() {
        KontekstHolder.setKontekst(BasisKontekst.ikkeAutentisertRequest("fp-inntektsmelding"));
        System.setProperty("altinn.tre.inntektsmelding.ressurs", NAV_TEST_RESSURS);
    }

    @AfterEach
    void tearDown() {
        KontekstHolder.fjernKontekst();
        System.clearProperty("altinn.tre.inntektsmelding.ressurs");
    }

    @Test
    void sjekkTilgang__har_tilgang_til_en_bedrift_altinn_to_tjeneste_ok() {
        var altinnAutoriseringKlient = new ArbeidsgiverAltinnTilgangerKlient(klient);

        when(klient.send(any(RestRequest.class), any())).thenReturn(lagOrgNrTilTilgangResponse(TEST_ORGNR, NAV_TEST_RESSURS, ALTINN_TO_TJENESTE));
        assertThat(altinnAutoriseringKlient.harTilgangTilBedriften(TEST_ORGNR, false)).isTrue();
        verify(klient).send(any(RestRequest.class), any());
    }

    @Test
    void sjekkTilgang__har_tilgang_til_en_bedrift_altinn_to_tjeneste_nok() {
        var altinnAutoriseringKlient = new ArbeidsgiverAltinnTilgangerKlient(klient);

        when(klient.send(any(RestRequest.class), any())).thenReturn(lagOrgNrTilTilgangResponse(TEST_ORGNR, NAV_TEST_RESSURS, ALTINN_TO_TJENESTE));
        assertThat(altinnAutoriseringKlient.harTilgangTilBedriften("000000000", false)).isFalse();
        verify(klient).send(any(RestRequest.class), any());
    }

    @Test
    void sjekkTilgang__har_tilgang_til_en_bedrift_altinn_tre_ressurs_ok() {
        var altinnAutoriseringKlient = new ArbeidsgiverAltinnTilgangerKlient(klient);

        when(klient.send(any(RestRequest.class), any())).thenReturn(lagOrgNrTilTilgangResponse(TEST_ORGNR, NAV_TEST_RESSURS, ALTINN_TO_TJENESTE));
        assertThat(altinnAutoriseringKlient.harTilgangTilBedriften(TEST_ORGNR, true)).isTrue();
        verify(klient).send(any(RestRequest.class), any());
    }

    @Test
    void sjekkTilgang__har_tilgang_til_en_bedrift_altinn_tre_ressurs_nok() {
        var altinnAutoriseringKlient = new ArbeidsgiverAltinnTilgangerKlient(klient);

        when(klient.send(any(RestRequest.class), any())).thenReturn(lagOrgNrTilTilgangResponse(TEST_ORGNR, NAV_TEST_RESSURS, ALTINN_TO_TJENESTE));
        assertThat(altinnAutoriseringKlient.harTilgangTilBedriften("000000000", true)).isFalse();
        verify(klient).send(any(RestRequest.class), any());
    }

    @Test
    void sjekkTilgang__hent_liste_med_bedrifter_med_tilgang_til_altinn_3_ressurs_ok() {
        var altinnAutoriseringKlient = new ArbeidsgiverAltinnTilgangerKlient(klient);
        when(klient.send(any(RestRequest.class), any())).thenReturn(lagTilgangTilOrgNrResponse(NAV_TEST_RESSURS, TEST_ORGNR));
        assertThat(altinnAutoriseringKlient.hentBedrifterArbeidsgiverHarTilgangTil(true)).isNotEmpty().contains(TEST_ORGNR);
        verify(klient).send(any(RestRequest.class), any());
    }

    @Test
    void sjekkTilgang__hent_liste_med_bedrifter_med_tilgang_til_altinn_3_ressurs_nok() {
        var altinnAutoriseringKlient = new ArbeidsgiverAltinnTilgangerKlient(klient);
        when(klient.send(any(RestRequest.class), any())).thenReturn(lagTilgangTilOrgNrResponse(ALTINN_TO_TJENESTE, TEST_ORGNR));
        assertThat(altinnAutoriseringKlient.hentBedrifterArbeidsgiverHarTilgangTil(true)).isEmpty();
        verify(klient).send(any(RestRequest.class), any());
    }

    @Test
    void sjekkTilgang__hent_liste_med_bedrifter_med_tilgang_til_altinn_3_ressurs_ikke_tilgang_til_bedrift_nok() {
        var altinnAutoriseringKlient = new ArbeidsgiverAltinnTilgangerKlient(klient);
        when(klient.send(any(RestRequest.class), any())).thenReturn(lagTilgangTilOrgNrResponse(ALTINN_TO_TJENESTE, "000000000"));
        assertThat(altinnAutoriseringKlient.hentBedrifterArbeidsgiverHarTilgangTil(true)).isEmpty();
        verify(klient).send(any(RestRequest.class), any());
    }

    @Test
    void sjekkTilgang__hent_liste_med_bedrifter_med_tilgang_til_altinn_2_tjeneste_ok() {
        var altinnAutoriseringKlient = new ArbeidsgiverAltinnTilgangerKlient(klient);
        when(klient.send(any(RestRequest.class), any())).thenReturn(lagTilgangTilOrgNrResponse(ALTINN_TO_TJENESTE, TEST_ORGNR));
        assertThat(altinnAutoriseringKlient.hentBedrifterArbeidsgiverHarTilgangTil(false)).isNotEmpty().contains(TEST_ORGNR);
        verify(klient).send(any(RestRequest.class), any());
    }

    @Test
    void sjekkTilgang__hent_liste_med_bedrifter_med_tilgang_til_altinn_2_tjeneste_nok() {
        var altinnAutoriseringKlient = new ArbeidsgiverAltinnTilgangerKlient(klient);
        when(klient.send(any(RestRequest.class), any())).thenReturn(lagTilgangTilOrgNrResponse(NAV_TEST_RESSURS, TEST_ORGNR));
        assertThat(altinnAutoriseringKlient.hentBedrifterArbeidsgiverHarTilgangTil(false)).isEmpty();
        verify(klient).send(any(RestRequest.class), any());
    }

    @Test
    void sjekkTilgang__hent_liste_med_bedrifter_med_tilgang_til_altinn_2_tjeneste_ikke_tilgang_til_bedrift_nok() {
        var altinnAutoriseringKlient = new ArbeidsgiverAltinnTilgangerKlient(klient);
        when(klient.send(any(RestRequest.class), any())).thenReturn(lagTilgangTilOrgNrResponse(NAV_TEST_RESSURS, "000000000"));
        assertThat(altinnAutoriseringKlient.hentBedrifterArbeidsgiverHarTilgangTil(false)).isEmpty();
        verify(klient).send(any(RestRequest.class), any());
    }

    private ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse lagOrgNrTilTilgangResponse(String orgnr, String... tilganger) {
        return new ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse(false, List.of(), Map.of(orgnr, List.of(tilganger)), null);
    }

    private ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse lagTilgangTilOrgNrResponse(String tilgang, String... orgnre) {
        return new ArbeidsgiverAltinnTilgangerKlient.ArbeidsgiverAltinnTilgangerResponse(false, List.of(), null, Map.of(tilgang, List.of(orgnre)));
    }
}
