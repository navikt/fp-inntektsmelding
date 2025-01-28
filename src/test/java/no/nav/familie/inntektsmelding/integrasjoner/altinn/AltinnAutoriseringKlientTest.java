package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import static no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnAutoriseringKlient.ALTINN_SIZE_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.IntStream;

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
class AltinnAutoriseringKlientTest {
    @Mock
    RestClient klient;

    @BeforeEach
    void setUp() {
        KontekstHolder.setKontekst(BasisKontekst.ikkeAutentisertRequest("fp-inntektsmelding"));
    }

    @Test
    void sjekkTilgang__har_tilgang_til_en_bedrift() {
        var altinnAutoriseringKlient = new AltinnAutoriseringKlient(klient);

        when(klient.sendReturnList(any(RestRequest.class), any())).thenReturn(List.of(lagAltinnReportee("Saltrød og høneby", "999999999")));

        altinnAutoriseringKlient.harTilgangTilBedriften("999999999");

        verify(klient).sendReturnList(any(RestRequest.class), any());
    }

    @Test
    void sjekkTilgang__ikke_tilgang_til_en_bedrift() {
        var altinnAutoriseringKlient = new AltinnAutoriseringKlient(klient);

        when(klient.sendReturnList(any(RestRequest.class), any())).thenReturn(List.of(lagAltinnReportee("Saltrød og høneby", "999999999")));

        assertThat(altinnAutoriseringKlient.harTilgangTilBedriften("000000000")).isFalse();
        verify(klient).sendReturnList(any(RestRequest.class), any());
    }

    @Test
    void sjekkTilgang__har_tilgang_til_bedrift_ved_paginering() {
        var altinnAutoriseringKlient = new AltinnAutoriseringKlient(klient);

        var side1 = IntStream.rangeClosed(1, ALTINN_SIZE_LIMIT)
            .boxed()
            .map(i -> lagAltinnReportee("Bedrift nr " + i, String.valueOf(999999000 + i)))
            .toList();
        var side2 = List.of(lagAltinnReportee("Bedrift på side 2 AS", "999999999"));
        when(klient.sendReturnList(any(RestRequest.class), eq(AltinnAutoriseringKlient.AltinnReportee.class))).thenReturn(side1, side2);

        altinnAutoriseringKlient.harTilgangTilBedriften("999999999");

        verify(klient, times(2)).sendReturnList(any(RestRequest.class), any());
    }

    private static AltinnAutoriseringKlient.AltinnReportee lagAltinnReportee(String name, String orgnr) {
        return new AltinnAutoriseringKlient.AltinnReportee(name, "BEDR", orgnr, "900000000", "", "ACTIVE", "BEDR");
    }


}
