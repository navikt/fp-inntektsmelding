package no.nav.familie.inntektsmelding.integrasjoner.dokgen.v1;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.familie.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "ny.fpdokgen.url", endpointDefault = "http://fpdokgen",
    scopesProperty = "ny.fpdokgen.scopes", scopesDefault = "api://prod-gcp.teamforeldrepenger.fp-dokgen/.default")
public class NyFpDokgenRestKlient {

    protected static final String API_PATH = "/api";
    private static final String V1_GENERER_PATH = "/v1/dokument/generer";

    private final RestClient restClient;
    private final RestConfig restConfig;

    public NyFpDokgenRestKlient() {
        this(RestClient.client());
    }

    public NyFpDokgenRestKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(NyFpDokgenRestKlient.class);
    }

    public byte[] genererPdf(InntektsmeldingPdfData metadata, ForespørselType forespørselType) {
        var template = utledMal(forespørselType);

        var endpoint = UriBuilder.fromUri(restConfig.endpoint()).path(API_PATH).path(V1_GENERER_PATH).path("/pdf").build();
        var requestDto = new NyDokgenRequest(template, null, NyDokgenRequest.CssStyling.INNTEKTSMELDING_PDF,
            DefaultJsonMapper.toJson(metadata));

        var request = RestRequest.newPOSTJson(requestDto, endpoint, restConfig)
            .header(HttpHeaders.ACCEPT, "application/pdf");

        var pdf = restClient.sendReturnByteArray(request);

        if (pdf == null || pdf.length == 0) {
            throw new TekniskException("FPIM", "Fikk tomt svar ved kall til dokgen for generering av pdf for inntektsmelding");
        }
        return pdf;
    }

    private String utledMal(ForespørselType forespørselType) {
        if (forespørselType == ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT) {
            return "fpinntektsmelding-refusjonskrav";
        }
        return "fpinntektsmelding-inntektsmelding";
    }
}
