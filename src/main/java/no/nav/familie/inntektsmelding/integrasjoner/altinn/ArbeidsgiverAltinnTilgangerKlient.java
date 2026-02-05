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

    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsgiverAltinnTilgangerKlient.class);

    private static final Environment ENV = Environment.current();

    public static final String ALTINN_TO_TJENESTE = "4936:1";
    public static final String ALTINN_TRE_RESSURS = ENV.getRequiredProperty("altinn.tre.inntektsmelding.ressurs");

    public static final boolean BRUK_ALTINN_TRE_FOR_TILGANGSKONTROLL = ENV.getProperty("bruk.altinn.tre.for.tilgangskontroll.toggle", boolean.class, true);

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

    // TODO: Må ryddes opp etter Altinn 3 ressurs overgang i prod.
    public boolean harTilgangTilBedriften(String orgnr) {
        var altinnRessurserBrukerHarTilgangTilPerOrgnr = hentTilganger().orgNrTilTilganger();

        if (altinnRessurserBrukerHarTilgangTilPerOrgnr == null || altinnRessurserBrukerHarTilgangTilPerOrgnr.isEmpty()
            || !altinnRessurserBrukerHarTilgangTilPerOrgnr.containsKey(orgnr)) {
            SECURE_LOG.info("ALTINN: Bruker har ikke tilgang til orgnr: {}", orgnr);
            return false;
        }

        var brukersTilgangerForOrgnr = altinnRessurserBrukerHarTilgangTilPerOrgnr.get(orgnr);

        var tilgangsbeslutningAltinn2 = brukersTilgangerForOrgnr.contains(ALTINN_TO_TJENESTE);
        var tilgangsbeslutningAltinn3 = brukersTilgangerForOrgnr.contains(ALTINN_TRE_RESSURS);

        if (tilgangsbeslutningAltinn2 != tilgangsbeslutningAltinn3) { // hvis tilgang er ulikt mellom Altinn 2 og Altinn 3, logg for avstemming.
            LOG.info("ALTINN: Tilgangsbeslutninger er ulike for bruker! Altinn 2: {}, Altinn 3: {}.", tilgangsbeslutningAltinn2, tilgangsbeslutningAltinn3);
            SECURE_LOG.info("ALTINN: Brukers tilganger for orgnr {}: {}", orgnr, brukersTilgangerForOrgnr);
        }
        return BRUK_ALTINN_TRE_FOR_TILGANGSKONTROLL ? tilgangsbeslutningAltinn3 : tilgangsbeslutningAltinn2;
    }

    // TODO: Må ryddes opp etter Altinn 3 ressurs overgang i prod.
    public List<String> hentBedrifterArbeidsgiverHarTilgangTil() {
        var orgNrBrukerHarTilgangTilPerRessurs = hentTilganger().tilgangTilOrgNr();

        var orgNrMedGittTilgangIAltinn2 = hentSortertListeMedOrgNrMedGittTilgang(orgNrBrukerHarTilgangTilPerRessurs, ALTINN_TO_TJENESTE);
        var orgNrMedGittTilgangIAltinn3 = hentSortertListeMedOrgNrMedGittTilgang(orgNrBrukerHarTilgangTilPerRessurs, ALTINN_TRE_RESSURS);

        if (!orgNrMedGittTilgangIAltinn2.equals(orgNrMedGittTilgangIAltinn3)) { // listene må være sortert for å kunne sammenlignes direkte.
            LOG.info("ALTINN: Uoverensstemmelse i lister over bedrifter bruker har tilgang til mellom Altinn 2 og Altinn 3.");
            SECURE_LOG.info("ALTINN: Bruker har tilgang til følgende bedrifter: Altinn2: {}, Altinn3: {}", orgNrMedGittTilgangIAltinn2, orgNrMedGittTilgangIAltinn3);
        }
        return BRUK_ALTINN_TRE_FOR_TILGANGSKONTROLL ? orgNrMedGittTilgangIAltinn3 : orgNrMedGittTilgangIAltinn2;
    }

    private static List<String> hentSortertListeMedOrgNrMedGittTilgang(Map<String, List<String>> orgNrBrukerHarTilgangTilPerRessurs, String ressurs) {
        return orgNrBrukerHarTilgangTilPerRessurs.getOrDefault(ressurs, List.of()).stream().sorted().toList();
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
