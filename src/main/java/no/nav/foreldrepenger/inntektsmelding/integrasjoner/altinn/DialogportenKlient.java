package no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
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
    private final RestClient restClient;
    private final RestConfig restConfig;
    private final AltinnExchangeTokenKlient tokenKlient;
    private final String inntektsmeldingSkjemaLenke;
    private final String inntektsmeldingApiLenke;
    private final String forespørselApiLenke;
    private final String dokumentasjonsLenke;

    DialogportenKlient() {
        this(RestClient.client());
    }

    public DialogportenKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
        this.tokenKlient = AltinnExchangeTokenKlient.instance();
        this.inntektsmeldingSkjemaLenke = ENV.getProperty("inntektsmelding.skjema.lenke", "https://arbeidsgiver.nav.no/fp-im-dialog");
        this.inntektsmeldingApiLenke = ENV.getProperty("inntektsmelding.api.lenke", "https://foreldrepenger-inntektsmelding-api.ekstern.nav.no/v1/inntektsmelding/send-inn");
        this.forespørselApiLenke = ENV.getProperty("foresporsel.api.lenke", "https://foreldrepenger-inntektsmelding-api.ekstern.nav.no/v1/foresporsel-ekstern/hent");
        this.dokumentasjonsLenke = ENV.getProperty("inntektsmelding.dokumentasjon.lenke", "https://foreldrepenger-inntektsmelding-api.ekstern.nav.no/forvaltning/api/openapi.json");
    }

    public String opprettDialog(UUID forespørselUuid,
                                Arbeidsgiver arbeidsgiver,
                                String sakstittel,
                                LocalDate førsteUttaksdato,
                                Ytelsetype ytelsetype) {
        var target = URI.create(restConfig.endpoint().toString() + "/dialogporten/api/v1/serviceowner/dialogs");
        var opprettRequest = DialogportenRequestMapper.opprettDialogRequest(arbeidsgiver,
            forespørselUuid,
            sakstittel,
            førsteUttaksdato,
            ytelsetype,
            inntektsmeldingSkjemaLenke,
            inntektsmeldingApiLenke,
            forespørselApiLenke,
            dokumentasjonsLenke);
        var request = RestRequest.newPOSTJson(opprettRequest, target, restConfig)
            .otherAuthorizationSupplier(() -> tokenKlient.hentAltinnToken(this.restConfig.scopes()));

        var response = restClient.sendReturnUnhandled(request);
        return handleResponse(response);
    }

    public void ferdigstillDialog(UUID dialogUuid,
                                  Arbeidsgiver arbeidsgiver,
                                  String sakstittel,
                                  Ytelsetype ytelsetype,
                                  LocalDate førsteUttaksdato,
                                  Optional<UUID> inntektsmeldingUuid,
                                  LukkeÅrsak lukkeÅrsak) {
        var patchRequestFerdig = DialogportenRequestMapper.opprettFerdigstillPatchRequest(sakstittel,
            arbeidsgiver,
            ytelsetype,
            førsteUttaksdato,
            inntektsmeldingUuid,
            lukkeÅrsak,
            inntektsmeldingSkjemaLenke);
        sendPatchRequest(dialogUuid, patchRequestFerdig);
    }

    public void oppdaterDialogMedEndretInntektsmelding(UUID dialogUuid,
                                                       Arbeidsgiver arbeidsgiver,
                                                       Optional<UUID> inntektsmeldingUuid) {
        var patchRequestInnsendt = DialogportenRequestMapper.opprettInnsendtInntektsmeldingPatchRequest(
            arbeidsgiver,
            inntektsmeldingUuid,
            inntektsmeldingSkjemaLenke);
        sendPatchRequest(dialogUuid, patchRequestInnsendt);
    }

    public void settDialogTilUtgått(UUID dialogUuid, String sakstittel) {
        var patchRequestUtgått = DialogportenRequestMapper.opprettUtgåttPatchRequest(sakstittel);
        sendPatchRequest(dialogUuid, patchRequestUtgått);
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
}
