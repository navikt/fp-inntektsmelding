package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.konfig.Environment;
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

    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsgiverAltinnTilgangerKlient.class);
    private static final Environment ENV = Environment.current();

    public static final String ALTINN_TO_TJENESTE = "4936:1";
    public static final String ALTINN_TRE_RESSURS = ENV.getRequiredProperty("altinn.tre.inntektsmelding.ressurs");
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

    public boolean harTilgangTilBedriften(String orgnr, boolean brukAltinnTreRessurs) {
        var orgNrTilTilganger = hentTilganger().orgNrTilTilganger();
        if (orgNrTilTilganger == null || orgNrTilTilganger.isEmpty() || !orgNrTilTilganger.containsKey(orgnr)) {
            LOG.debug("ALTINN: Bruker har ikke tilgang til orgnr: {}", orgnr);
            return false;
        }
        if (!brukAltinnTreRessurs) {
            if (orgNrTilTilganger.get(orgnr).contains(ALTINN_TRE_RESSURS)) {
                LOG.debug("ALTINN: Bruker har gyldig tilgang til ressurs: {} i orgnr: {}", ALTINN_TRE_RESSURS, orgnr);
            } else {
                LOG.debug("ALTINN: Bruker har ikke tilgang til ressurs: {} i orgnr: {}", ALTINN_TRE_RESSURS, orgnr);
            }
        }
        return orgNrTilTilganger.get(orgnr).contains(finnRiktigAltinnRessurs(brukAltinnTreRessurs));
    }

    public List<String> hentBedrifterArbeidsgiverHarTilgangTil(boolean brukAltinnTreRessurs) {
        var tilgangTilOrgNr = hentTilganger().tilgangTilOrgNr();

        if (!brukAltinnTreRessurs) {
            loggTilganger(tilgangTilOrgNr, ALTINN_TRE_RESSURS);
        } else {
            loggTilganger(tilgangTilOrgNr, ALTINN_TO_TJENESTE);
        }
        return tilgangTilOrgNr.getOrDefault(finnRiktigAltinnRessurs(brukAltinnTreRessurs), List.of())
            .stream()
            .sorted()
            .toList();
    }

    private static String finnRiktigAltinnRessurs(boolean brukAltinnTreRessurs) {
        return brukAltinnTreRessurs ? ALTINN_TRE_RESSURS : ALTINN_TO_TJENESTE;
    }

    private static void loggTilganger(Map<String, List<String>> tilgangTilOrgNr, String altinnRessurs) {
        LOG.debug("ALTINN: Bruker har tilgang til følgende bedrifter: {} gjennom: {}", tilgangTilOrgNr.getOrDefault(altinnRessurs, List.of()),
            altinnRessurs);
    }

    private ArbeidsgiverAltinnTilgangerResponse hentTilganger() {
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
            List.of(ALTINN_TO_TJENESTE),
            List.of(ALTINN_TRE_RESSURS)));
    }

    public record ArbeidsgiverAltinnTilgangerRequest(FilterCriteria filter) {
        public record FilterCriteria(@JsonProperty("altinn2Tilganger") List<String> altinn2Tilganger,
                                     @JsonProperty("altinn3Tilganger") List<String> altinn3Tilganger) {
        }
    }

    public record ArbeidsgiverAltinnTilgangerResponse(boolean isError,
                                                      List<Organisasjon> hierarki,
                                                      Map<String, List<String>> orgNrTilTilganger,
                                                      Map<String, List<String>> tilgangTilOrgNr) {

        public record Organisasjon(String orgnr,
                                   @JsonProperty("altinn3Tilganger") List<String> altinn3Tilganger,
                                   @JsonProperty("altinn2Tilganger") List<String> altinn2Tilganger,
                                   List<Organisasjon> underenheter,
                                   String navn,
                                   String organisasjonsform,
                                   boolean erSlettet) {
        }
    }
}
