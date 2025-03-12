package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;

class FpDokgenKlientTest {

    RestClient restClient = mock(RestClient.class);

    @Test
    void skal_generere_pdf()  {
        FpDokgenKlient fpDokgenKlient = new FpDokgenKlient(restClient);
        when(restClient.sendReturnByteArray(any())).thenReturn("pdf".getBytes());
        var bytes = fpDokgenKlient.genererPdf(new InntektsmeldingPdfData(), ForespørselType.BESTILT_AV_FAGSYSTEM);
        assertThat(bytes).isNotEmpty();
    }
}
