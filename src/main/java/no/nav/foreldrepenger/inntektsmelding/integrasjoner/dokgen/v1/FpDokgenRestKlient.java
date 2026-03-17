package no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen.v1;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPDOKGEN)
public class FpDokgenRestKlient {

    protected static final String API_PATH = "/api";
    private static final String V1_GENERER_PATH = "/v1/dokument/generer";

    private final RestClient restClient;
    private final RestConfig restConfig;

    public FpDokgenRestKlient() {
        this(RestClient.client());
    }

    public FpDokgenRestKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(FpDokgenRestKlient.class);
    }

    public byte[] genererPdf(FpDokgenRequest requestDto) {

        var endpoint = UriBuilder.fromUri(restConfig.fpContextPath()).path(API_PATH).path(V1_GENERER_PATH).path("/pdf").build();

        var request = RestRequest.newPOSTJson(requestDto, endpoint, restConfig)
            .header(HttpHeaders.ACCEPT, "application/pdf");

        var pdf = restClient.sendReturnByteArray(request);

        if (pdf == null || pdf.length == 0) {
            throw new TekniskException("IM-FPDOKGEN", "Fikk tomt svar ved kall til dokgen for generering av PDF for inntektsmelding");
        }
        return pdf;
    }
}
