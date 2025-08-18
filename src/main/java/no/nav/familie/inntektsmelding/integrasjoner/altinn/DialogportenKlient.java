package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED, endpointProperty = "altinn.tre.base.url", scopesProperty = "maskinporten.dialogporten.scope")
public class DialogportenKlient {
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

    public String opprettDialog(UUID forespørselUuid, OrganisasjonsnummerDto orgnr, String sakstittel) {
        var target = URI.create(restConfig.endpoint().toString() + "/dialogporten/api/v1/serviceowner/dialogs");
        var bodyRequest = lagDialogportenBody(orgnr, forespørselUuid, sakstittel);
        var request = RestRequest.newPOSTJson(bodyRequest, target, restConfig)
            .otherAuthorizationSupplier(() -> tokenKlient.hentAltinnToken(this.restConfig.scopes()));

        var response = restClient.sendReturnUnhandled(request);
        return handleResponse(response);
    }

    private String handleResponse(HttpResponse<String> response) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            String msg = String.format("Kall til Altinn dialogporten feilet med statuskode %s. Full feilmelding var: %s", response.statusCode(), response.body());
            throw new IntegrasjonException("FPINNTEKTSMELDING-542684", msg);
        }
    }

    private DialogportenRequest lagDialogportenBody(OrganisasjonsnummerDto organisasjonsnummer, UUID forespørselUuid, String sakstittel) {
        var party = String.format("urn:altinn:organization:identifier-no:%s", organisasjonsnummer.orgnr());
        var contentTransmission = new DialogportenRequest.Content(lagContentValue(sakstittel), lagContentValue("Nav trenger inntektsmelding"));
        var contentRequest = new DialogportenRequest.Content(lagContentValue("Send inn inntektsmelding"), lagContentValue("Send inn inntektsmelding"));
        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Request,
            DialogportenRequest.ExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender("ServiceOwner"),
            contentRequest,
            List.of());
        var apiAction = new DialogportenRequest.ApiAction("Hent forespørsel om inntektsmelding",
            List.of(new DialogportenRequest.Endpoint(inntektsmeldingSkjemaLenke + "/" + forespørselUuid.toString(), DialogportenRequest.HttpMethod.GET, null)), DialogportenRequest.ACTION_READ);
        var foreldrepengerRessurs = Environment.current().getProperty("altinn.tre.inntektsmelding.ressurs");
        var altinnressursFP = ALTINN_RESSURS_PREFIX + foreldrepengerRessurs;
        return new DialogportenRequest(altinnressursFP,
            party,
            forespørselUuid.toString(),
            DialogportenRequest.DialogStatus.RequiresAttention, contentTransmission,
            List.of(transmission),
            List.of(apiAction));
    }

    private DialogportenRequest.ContentValue lagContentValue(String verdi) {
        return new DialogportenRequest.ContentValue(List.of(new DialogportenRequest.ContentValueItem(verdi, DialogportenRequest.NB)), DialogportenRequest.TEXT_PLAIN);
    }

    public void ferdigstilleDialog(String dialogUuid) {
        var target = URI.create(restConfig.endpoint().toString() + "/dialogporten/api/v1/serviceowner/dialogs/" + dialogUuid);

        var contentRequest = new DialogportenRequest.Content(lagContentValue("Vi har mottatt inntektsmeldingen"), lagContentValue("Vi har mottatt inntektsmeldingen"));

        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Acceptance,
            DialogportenRequest.ExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender("ServiceOwner"),
            contentRequest,
            List.of());

        var patchStatus = new DialogportenPatchRequest(DialogportenPatchRequest.OP_REPLACE,
            DialogportenPatchRequest.PATH_STATUS,
            DialogportenRequest.DialogStatus.Completed);

        var patchTransmission = new DialogportenPatchRequest(DialogportenPatchRequest.OP_ADD,
            DialogportenPatchRequest.PATH_TRANSMISSIONS,
            List.of(transmission));

        var method = new RestRequest.Method(RestRequest.WebMethod.PATCH,
            HttpRequest.BodyPublishers.ofString(DefaultJsonMapper.toJson(List.of(patchStatus, patchTransmission))));
        var restRequest = RestRequest.newRequest(method, target, restConfig)
            .otherAuthorizationSupplier(() -> tokenKlient.hentAltinnToken(this.restConfig.scopes()));

        var response = restClient.sendReturnUnhandled(restRequest);

        handleResponse(response);
    }
}
