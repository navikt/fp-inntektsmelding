package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.vedtak.exception.IntegrasjonException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED, endpointProperty = "altinn.tre.base.url", scopesProperty = "maskinporten.dialogporten.scope")
public class DialogportenKlient {
    private static final Logger LOG = LoggerFactory.getLogger(DialogportenKlient.class);

    private static final Environment ENV = Environment.current();

    private static final String ALTINN_RESSURS_PREFIX = "urn:altinn:resource:";

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final AltinnExchangeTokenKlient tokenKlient;
    private String inntektsmeldingSkjemaLenke;

    DialogportenKlient() {
        this(RestClient.client());
    }

    public DialogportenKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
        this.tokenKlient = AltinnExchangeTokenKlient.instance();
        this.inntektsmeldingSkjemaLenke = ENV.getProperty("inntektsmelding.skjema.lenke", "https://arbeidsgiver.intern.dev.nav.no/fp-im-dialog");
    }

    public String opprettDialog(String orgnr, UUID forespørselUuid, String sakstittel) {
        var target = URI.create(restConfig.endpoint().toString() + "/dialogporten/api/v1/serviceowner/dialogs");
        var bodyRequest = lagDialogportenBody(orgnr, forespørselUuid.toString(), sakstittel);
        var request = RestRequest.newPOSTJson(bodyRequest, target, restConfig)
            .otherAuthorizationSupplier(() -> tokenKlient.hentAltinnToken(this.restConfig.scopes()));

        var response = restClient.sendReturnUnhandled(request);
        return handleResponse(response);
    }

    public String opprettDialog(ForespørselEntitet forespørselEntitet, String sakstittel) {
        return opprettDialog(forespørselEntitet.getOrganisasjonsnummer(), forespørselEntitet.getUuid(), sakstittel);
    }

    private String handleResponse(HttpResponse<String> response) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }
        LOG.warn("Kall til Altinn dialogporten feilet med statuskode {}. Full feilmelding var: {}", response.statusCode(), response.body());
        throw new IntegrasjonException("FPINNTEKTSMELDING-542684", "Feil ved kall til dialogporten");
    }

    private DialogportenRequest lagDialogportenBody(String organisasjonsnummer, String forespørselUuid, String sakstittel) {
        var party = String.format("urn:altinn:organization:identifier-no:%s", organisasjonsnummer);
        var contentTransmission = new DialogportenRequest.Content(lagContentValue(sakstittel), lagContentValue("Nav trenger inntektsmelding"));
        var contentRequest = new DialogportenRequest.Content(lagContentValue("Send inn inntektsmelding"), lagContentValue("Send inn inntektsmelding"));
        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Request,
            DialogportenRequest.ExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender("ServiceOwner"),
            contentRequest,
            List.of());
        var apiAction = new DialogportenRequest.ApiAction("Hent forespørsel om inntektsmelding",
            List.of(new DialogportenRequest.Endpoint(inntektsmeldingSkjemaLenke + "/" + forespørselUuid, DialogportenRequest.HttpMethod.GET, null)), DialogportenRequest.ACTION_READ);
        var foreldrepengerRessurs = Environment.current().getProperty("altinn.tre.inntektsmelding.ressurs");
        var altinnressursFP = ALTINN_RESSURS_PREFIX + foreldrepengerRessurs;
        return new DialogportenRequest(altinnressursFP,
            party,
            forespørselUuid,
            DialogportenRequest.DialogStatus.RequiresAttention, contentTransmission,
            List.of(transmission),
            List.of(apiAction));
    }
    private DialogportenRequest.ContentValue lagContentValue(String verdi) {
        return new DialogportenRequest.ContentValue(List.of(new DialogportenRequest.ContentValueItem(verdi, DialogportenRequest.NB)), DialogportenRequest.TEXT_PLAIN);
    }

}
