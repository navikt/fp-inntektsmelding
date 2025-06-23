package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.sikkerhet.oidc.token.impl.GeneriskTokenKlient;
import no.nav.vedtak.sikkerhet.oidc.token.impl.MaskinportenTokenKlient;

@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, endpointProperty = "altinn.tre.base.url", scopesProperty = "altinn.scopes")
public class AltinnDialogportenKlient {

    static final String CONTENT_TYPE = "Content-type";
    static final String APPLICATION_FORM_ENCODED = "application/x-www-form-urlencoded";
    static final String AUTHORIZATION = "Authorization";
    static final String BASIC_AUTH_HEADER_PREFIX = "Basic ";

    private static final String SCOPE = "digdir:dialogporten.serviceprovider";
    public static final String ALTINN_EXCHANGE_PATH = "/authentication/api/v1/exchange/maskinporten";

    private final RestClient restClient;
    private final RestConfig restConfig;
    private MaskinportenTokenKlient tokenKlient;


    private AltinnDialogportenKlient() {
        this(RestClient.client());
    }

    AltinnDialogportenKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
        this.tokenKlient = null;
    }

    public void opprettDialog() {
        if (tokenKlient == null) {
            tokenKlient = MaskinportenTokenKlient.instance();
        }
        var target = URI.create(restConfig.endpoint().toString() + "/dialogporten/api/v1/serviceowner/dialogs");
        var bodyRequest = lagDialogportenBody();
        var request = RestRequest.newPOSTJson(bodyRequest, target, restConfig)
            .otherAuthorizationSupplier(this::hentToken);

    }

    private DialogportenRequest lagDialogportenBody() {
        String orgnr = "999999999";
        var party = String.format("urn:altinn:organization:identifier-no:%s", orgnr);
        var contentTransmission = new DialogportenRequest.Content(lagContentValue("Inntektsmelding"), lagContentValue("Sammendrag"));
        var contentRequest = new DialogportenRequest.Content(lagContentValue("Forespørsel om inntektsmelding"), lagContentValue("Forespørsel om inntektsmelding"));
        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Request,
            DialogportenRequest.ExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender("ServiceOwner"),
            contentRequest);
        return new DialogportenRequest(Environment.current().getProperty("altinn.tre.inntektsmelding.ressurs"),
            party,
            "test",
            DialogportenRequest.DialogStatus.New, contentTransmission,
            List.of(transmission));
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
        return GeneriskTokenKlient.hentTokenRetryable(httpRequest, null, 3).access_token();
    }

    private HttpRequest lagHttpRequest(String bearerToken) {
        var tokenAltinn3ExchangeEndpoint = restConfig.endpoint().toString() + ALTINN_EXCHANGE_PATH;
        return HttpRequest.newBuilder()
            .header("Cache-Control", "no-cache")
            .headers(AUTHORIZATION, bearerToken)
            .header(CONTENT_TYPE, APPLICATION_FORM_ENCODED)
            .timeout(Duration.ofSeconds(3))
            .uri(URI.create(tokenAltinn3ExchangeEndpoint))
            .GET()
            .build();
    }

}
