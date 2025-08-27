package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
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

    public String opprettDialog(UUID forespørselUuid,
                                OrganisasjonsnummerDto orgnr,
                                String sakstittel,
                                LocalDate førsteUttaksdato,
                                Ytelsetype ytelsetype) {
        var target = URI.create(restConfig.endpoint().toString() + "/dialogporten/api/v1/serviceowner/dialogs");
        var bodyRequest = lagDialogportenBody(orgnr, forespørselUuid, sakstittel, førsteUttaksdato, ytelsetype);
        var request = RestRequest.newPOSTJson(bodyRequest, target, restConfig)
            .otherAuthorizationSupplier(() -> tokenKlient.hentAltinnToken(this.restConfig.scopes()));

        var response = restClient.sendReturnUnhandled(request);
        return handleResponse(response);
    }

    private String handleResponse(HttpResponse<String> response) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            String msg = String.format("Kall til Altinn dialogporten feilet med statuskode %s. Full feilmelding var: %s",
                response.statusCode(),
                response.body());
            throw new IntegrasjonException("FPINNTEKTSMELDING-542684", msg);
        }
    }

    private DialogportenRequest lagDialogportenBody(OrganisasjonsnummerDto organisasjonsnummer,
                                                    UUID forespørselUuid,
                                                    String sakstittel,
                                                    LocalDate førsteUttaksdato,
                                                    Ytelsetype ytelsetype) {
        var party = String.format("urn:altinn:organization:identifier-no:%s", organisasjonsnummer.orgnr());
        var foreldrepengerRessurs = Environment.current().getProperty("altinn.tre.inntektsmelding.ressurs");
        var altinnressursFP = ALTINN_RESSURS_PREFIX + foreldrepengerRessurs;

        //Oppretter dialog
        var summaryDialog = String.format("Nav trenger inntektsmelding for å behandle søknad om %s med startdato %s.",
            ytelsetype.name().toLowerCase(),
            førsteUttaksdato);
        var contentDialog = new DialogportenRequest.Content(lagContentValue(sakstittel), lagContentValue(summaryDialog));

        //Oppretter transmission
        var contentTransmission = new DialogportenRequest.Content(lagContentValue("Send inn inntektsmelding"), null);
        var attachementTransmission = new DialogportenRequest.Attachment(
            List.of(new DialogportenRequest.ContentValueItem("Innsending av inntektsmelding på min side - arbeidsgiver hos Nav",
                DialogportenRequest.NB)),
            List.of(new DialogportenRequest.Url(inntektsmeldingSkjemaLenke + "/" + forespørselUuid.toString(), DialogportenRequest.NB,
                DialogportenRequest.AttachmentUrlConsumerType.Gui)));
        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Request,
            DialogportenRequest.ExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender("ServiceOwner"),
            contentTransmission,
            List.of(attachementTransmission));

        //oppretter api action
        var apiAction = new DialogportenRequest.ApiAction(String.format("Innsending av inntektsmelding for %s med startdato %s",
            ytelsetype.name().toLowerCase(),
            førsteUttaksdato.format(DateTimeFormatter.ofPattern("dd.MM.yy"))),
            List.of(new DialogportenRequest.Endpoint(inntektsmeldingSkjemaLenke + "/" + forespørselUuid, DialogportenRequest.HttpMethod.GET, null)),
            DialogportenRequest.ACTION_READ);

        return new DialogportenRequest(altinnressursFP,
            party,
            forespørselUuid.toString(),
            DialogportenRequest.DialogStatus.RequiresAttention,
            contentDialog,
            List.of(transmission),
            List.of(apiAction));
    }

    private DialogportenRequest.ContentValue lagContentValue(String verdi) {
        return new DialogportenRequest.ContentValue(List.of(new DialogportenRequest.ContentValueItem(verdi, DialogportenRequest.NB)),
            DialogportenRequest.TEXT_PLAIN);
    }

    public void ferdigstilleMeldingIDialogporten(UUID dialogUuid, String sakstittel, Ytelsetype ytelsetype, LocalDate førsteUttaksdato) {
        //oppdatere status på meldingen til fullført
        var patchStatus = new DialogportenPatchRequest(DialogportenPatchRequest.OP_REPLACE,
            DialogportenPatchRequest.PATH_STATUS,
            DialogportenRequest.DialogStatus.Completed);

        //oppdatere innholdet i dialogen
        var summaryDialog = String.format("Nav har mottatt inntektsmelding for søknad om %s med startdato %s",
            ytelsetype.name().toLowerCase(),
            førsteUttaksdato.format(
                DateTimeFormatter.ofPattern("dd.MM.yy")));
        var contentRequest = new DialogportenRequest.Content(lagContentValue(sakstittel), lagContentValue(summaryDialog));
        var patchContent = new DialogportenPatchRequest(DialogportenPatchRequest.OP_REPLACE,
            DialogportenPatchRequest.PATH_CONTENT,
            contentRequest);

        //Ny transmission som sier at inntektsmelding er mottatt
        var transmissionContent = new DialogportenRequest.Content(lagContentValue("Inntektsmelding mottatt"), null);
        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Acceptance,
            DialogportenRequest.ExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender("ServiceOwner"),
            transmissionContent,
            List.of());
        var patchTransmission = new DialogportenPatchRequest(DialogportenPatchRequest.OP_ADD,
            DialogportenPatchRequest.PATH_TRANSMISSIONS,
            List.of(transmission));

        sendPatchRequest(dialogUuid, List.of(patchStatus, patchContent, patchTransmission));
    }

    private void sendPatchRequest(UUID dialogUuid, List<DialogportenPatchRequest> oppdateringer) {
        var target = URI.create(restConfig.endpoint().toString() + "/dialogporten/api/v1/serviceowner/dialogs/" + dialogUuid);

        var method = new RestRequest.Method(RestRequest.WebMethod.PATCH,
            HttpRequest.BodyPublishers.ofString(DefaultJsonMapper.toJson(oppdateringer)));
        var restRequest = RestRequest.newRequest(method, target, restConfig)
            .otherAuthorizationSupplier(() -> tokenKlient.hentAltinnToken(this.restConfig.scopes()));

        var response = restClient.sendReturnUnhandled(restRequest);

        handleResponse(response);
    }

    public void settMeldingTilUtgåttIDialogporten(UUID dialogUuid, String sakstittel) {
        //oppdatere status på meldingen til not applicable
        var patchStatus = new DialogportenPatchRequest(DialogportenPatchRequest.OP_REPLACE,
            DialogportenPatchRequest.PATH_STATUS,
            DialogportenRequest.DialogStatus.NotApplicable);

        //oppdatere innholdet i dialogen
        var contentRequest = new DialogportenRequest.Content(lagContentValue(sakstittel), lagContentValue("Nav trenger ikke lenger denne inntektsmeldingen"));
        var patchContent = new DialogportenPatchRequest(DialogportenPatchRequest.OP_REPLACE,
            DialogportenPatchRequest.PATH_CONTENT,
            contentRequest);

        //Ny transmission som sier at inntektsmelding ikke lenger er påkrevd
        var transmissionContent = new DialogportenRequest.Content(lagContentValue("Inntektsmeldingen er ikke lenger påkrevd"), null);
        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Correction,
            DialogportenRequest.ExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender("ServiceOwner"),
            transmissionContent,
            List.of());
        var patchTransmission = new DialogportenPatchRequest(DialogportenPatchRequest.OP_ADD,
            DialogportenPatchRequest.PATH_TRANSMISSIONS,
            List.of(transmission));

        sendPatchRequest(dialogUuid, List.of(patchStatus, patchContent, patchTransmission));

    }
}
