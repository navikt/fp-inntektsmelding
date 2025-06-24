package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.integrasjon.rest.*;
import no.nav.vedtak.sikkerhet.oidc.token.impl.GeneriskTokenKlient;
import no.nav.vedtak.sikkerhet.oidc.token.impl.MaskinportenTokenKlient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;

@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED, endpointProperty = "altinn.tre.base.url", scopesProperty = "altinn.scopes")
public class AltinnDialogportenKlient {

    private static final Logger LOG = LoggerFactory.getLogger(AltinnDialogportenKlient.class);

    static final String CONTENT_TYPE = "Content-type";
    static final String APPLICATION_FORM_ENCODED = "application/x-www-form-urlencoded";
    static final String AUTHORIZATION = "Authorization";

    private static final String SCOPE = "digdir:dialogporten.serviceprovider";
    public static final String ALTINN_EXCHANGE_PATH = "/authentication/api/v1/exchange/maskinporten";

    private final RestClient restClient;
    private final RestConfig restConfig;
    private MaskinportenTokenKlient tokenKlient;
    private String inntektsmeldingSkjemaLenke;

    private static AltinnDialogportenKlient instance;

    private AltinnDialogportenKlient() {
        this(RestClient.client());
    }

    AltinnDialogportenKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
        this.tokenKlient = null;
        this.inntektsmeldingSkjemaLenke = Environment.current().getProperty("inntektsmelding.skjema.lenke", "https://arbeidsgiver.intern.dev.nav.no/fp-im-dialog");
    }

    public String opprettDialog(String organisasjonsnummer, String forespørselUuid) {
        if (tokenKlient == null) {
            tokenKlient = MaskinportenTokenKlient.instance();
        }
        var target = URI.create(restConfig.endpoint().toString() + "/dialogporten/api/v1/serviceowner/dialogs");
        var bodyRequest = lagDialogportenBody(organisasjonsnummer, forespørselUuid);
        var request = RestRequest.newPOSTJson(bodyRequest, target, restConfig)
            .otherAuthorizationSupplier(this::hentToken);

         return restClient.sendReturnResponseString(request).body();
    }

    private DialogportenRequest lagDialogportenBody(String organisasjonsnummer, String forespørselUuid) {
        var party = String.format("urn:altinn:organization:identifier-no:%s", organisasjonsnummer);
        var contentTransmission = new DialogportenRequest.Content(lagContentValue("Inntektsmelding"), lagContentValue("Sammendrag"));
        var contentRequest = new DialogportenRequest.Content(lagContentValue("Forespørsel om inntektsmelding"), lagContentValue("Forespørsel om inntektsmelding"));
        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Request,
            DialogportenRequest.ExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender("ServiceOwner"),
            contentRequest,
            List.of());
        var apiAction = new DialogportenRequest.ApiAction("Hent forespørsel om inntektsmelding",
            List.of(new DialogportenRequest.Endpoint(inntektsmeldingSkjemaLenke + "/" + forespørselUuid, DialogportenRequest.HttpMethod.GET, null)));
        return new DialogportenRequest(Environment.current().getProperty("altinn.tre.inntektsmelding.ressurs"),
            party,
            forespørselUuid,
            DialogportenRequest.DialogStatus.RequiresAttention, contentTransmission,
            List.of(transmission),
            List.of(apiAction));
    }

    private DialogportenRequest.ContentValue lagContentValue(String verdi) {
        return new DialogportenRequest.ContentValue(List.of(new DialogportenRequest.ContentValueItem(verdi)));
    }

    private String hentToken() {
        var maskinportenToken = tokenKlient.hentMaskinportenToken(SCOPE, null).token();
        return veksleTilAltinn3Token(maskinportenToken);
    }

    private String veksleTilAltinn3Token(String token) {
        var httpRequest = lagHttpRequest(token);
        var response = GeneriskTokenKlient.hentTokenRetryable(httpRequest, null, 3);
        LOG.debug("Altinn leverte token av type {} utløper {}", response.token_type(), response.expires_in());
        return response.access_token();
    }

    private HttpRequest lagHttpRequest(String bearerToken) {
        var tokenAltinn3ExchangeEndpoint = restConfig.endpoint().toString() + ALTINN_EXCHANGE_PATH;
        return HttpRequest.newBuilder()
            .header("Cache-Control", "no-cache")
            .header(AUTHORIZATION, bearerToken)
            .header(CONTENT_TYPE, APPLICATION_FORM_ENCODED)
            .timeout(Duration.ofSeconds(3))
            .uri(URI.create(tokenAltinn3ExchangeEndpoint))
            .GET()
            .build();
    }

    public static synchronized AltinnDialogportenKlient instance() {
        var inst = instance;
        if (inst == null) {
            inst = new AltinnDialogportenKlient();
            instance = inst;
        }
        return inst;
    }

}
