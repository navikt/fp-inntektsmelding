package no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.rest.NavHeaders;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

// Denne klienten opererer med TokenX derfor trenger man en resource.
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, endpointProperty = "arbeidsgiver.altinn.tilganger.url", scopesProperty = "arbeidsgiver.altinn.tilganger.resource")
public class ArbeidsgiverAltinnTilgangerKlient {

    private static final String ALTINN_TILGANGER_PATH = "/altinn-tilganger";

    private static ArbeidsgiverAltinnTilgangerKlient instance = new ArbeidsgiverAltinnTilgangerKlient();

    private final RestClient restClient;
    private final RestConfig restConfig;

    private ArbeidsgiverAltinnTilgangerKlient() {
        this(RestClient.client());
    }

    ArbeidsgiverAltinnTilgangerKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public static synchronized ArbeidsgiverAltinnTilgangerKlient instance() {
        var inst = instance;
        if (inst == null) {
            inst = new ArbeidsgiverAltinnTilgangerKlient();
            instance = inst;
        }
        return inst;
    }

    public ArbeidsgiverAltinnTilgangerResponse hentTilganger() {
        var uri = UriBuilder.fromUri(restConfig.endpoint()).path(ALTINN_TILGANGER_PATH).build();
        var request = RestRequest.newPOSTJson(lagRequestFilter(), uri, restConfig);
        request.otherCallId(NavHeaders.HEADER_NAV_CORRELATION_ID);
        try {
            return restClient.send(request, ArbeidsgiverAltinnTilgangerResponse.class);
        } catch (RuntimeException e) {
            throw new IntegrasjonException("FP-965432",
                "Feil ved kall til arbeidsgiver-altinn-tilganger. Meld til #team_fager hvis dette skjer over lengre tidsperiode.", e);
        }
    }

    private ArbeidsgiverAltinnTilgangerRequest lagRequestFilter() {
        return new ArbeidsgiverAltinnTilgangerRequest(new ArbeidsgiverAltinnTilgangerRequest.FilterCriteria(
            List.of(AltinnRessurser.ALTINN_TO_TJENESTE), //TODO: Kan fjernes når Altinn 3 er på plass for alle tjenester
            List.of(AltinnRessurser.ALTINN_TRE_INNTEKTSMELDING_RESSURS)));
    }

    private record ArbeidsgiverAltinnTilgangerRequest(FilterCriteria filter) {
        private record FilterCriteria(@JsonProperty("altinn2Tilganger") List<String> altinn2Tilganger, //TODO: Kan fjernes når Altinn 3 er på plass for alle tjenester
                                     @JsonProperty("altinn3Tilganger") List<String> altinn3Tilganger) {
        }
    }

    public record ArbeidsgiverAltinnTilgangerResponse(boolean isError,
                                                      List<Organisasjon> hierarki,
                                                      Map<String, List<String>> orgNrTilTilganger,
                                                      Map<String, List<String>> tilgangTilOrgNr) {

        public record Organisasjon(String orgnr,
                                   @JsonProperty("altinn3Tilganger") List<String> altinn3Tilganger,
                                   @JsonProperty("altinn2Tilganger") List<String> altinn2Tilganger, //TODO: Kan fjernes når Altinn 3 er på plass for alle tjenester
                                   List<Organisasjon> underenheter,
                                   String navn,
                                   String organisasjonsform,
                                   boolean erSlettet) {
        }
    }
}
