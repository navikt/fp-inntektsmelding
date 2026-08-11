package no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;

class FpDokgenKlientTest {

    RestClient restClient = mock(RestClient.class);

    @Test
    void skal_generere_pdf() {
        var fpDokgenKlient = new FpDokgenRestKlient(restClient);
        when(restClient.sendReturnByteArray(any())).thenReturn("pdf".getBytes());
        var bytes = fpDokgenKlient.genererPdf(new FpDokgenRequest("testMal", FpDokgenRequest.Språk.BOKMÅL, FpDokgenRequest.CssStyling.PDF, "input"));
        assertThat(bytes).isNotEmpty();
    }
}
