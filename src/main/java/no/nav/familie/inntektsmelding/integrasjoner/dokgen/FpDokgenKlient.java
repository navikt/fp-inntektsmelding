package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED,
    endpointProperty = "fpdokgen.url",
    endpointDefault = "http://fpdokgen.teamforeldrepenger",
    application = FpApplication.FPDOKGEN)
public class FpDokgenKlient {
    private final RestClient restClient;
    private final RestConfig restConfig;

    public FpDokgenKlient() {
        this(RestClient.client());
    }

    public FpDokgenKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(FpDokgenKlient.class);
    }

    public byte[] genererPdf(InntektsmeldingPdfData dokumentdata, ForespørselType forespørselType) {
        var templatePath = utledTemplatePath(forespørselType);
        var endpoint = UriBuilder.fromUri(restConfig.endpoint()).path(templatePath).path("/create-pdf-format-variation").build();
        var request = RestRequest.newPOSTJson(dokumentdata, endpoint, restConfig);
        var pdf = restClient.sendReturnByteArray(request);

        if (pdf == null || pdf.length == 0) {
            throw new TekniskException("FPIM", "Fikk tomt svar ved kall til dokgen for generering av pdf for inntektsmelding");
        }
        return pdf;
    }

    private String utledTemplatePath(ForespørselType forespørselType) {
        if (forespørselType == ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT) {
            return "/template/fpinntektsmelding-refusjonskrav/PDFINNTEKTSMELDING";
        }
        return "/template/fpinntektsmelding-inntektsmelding/PDFINNTEKTSMELDING";
    }
}
