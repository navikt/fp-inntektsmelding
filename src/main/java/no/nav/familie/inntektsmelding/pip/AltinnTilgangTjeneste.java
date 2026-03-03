package no.nav.familie.inntektsmelding.pip;

import java.util.List;
import java.util.Map;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;

import jakarta.enterprise.context.Dependent;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnRessurser;
import no.nav.familie.inntektsmelding.integrasjoner.altinn.ArbeidsgiverAltinnTilgangerKlient;

import no.nav.foreldrepenger.konfig.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnRessurser.ALTINN_TO_TJENESTE;
import static no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnRessurser.ALTINN_TRE_INNTEKTSMELDING_RESSURS;

@Dependent
public class AltinnTilgangTjeneste {

    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");
    private static final Logger LOG = LoggerFactory.getLogger(AltinnTilgangTjeneste.class);
    private static final Environment ENV = Environment.current();

    private static final String APP_NAME = ENV.getNaisAppName().replace("-", "_");
    private static final String COUNTER_TILGANG_BEDRIFT = APP_NAME + ".tilgang_til_org";
    private static final String COUNTER_HENT_BEDRIFTER = APP_NAME + ".hent_org_liste";
    private static final String TAG_TILGANG_OK = "tilgang_ok";
    private static final String TAG_LIK = "lik_svar";

    public static final boolean BRUK_ALTINN_TRE_FOR_TILGANGSKONTROLL = ENV.getProperty("bruk.altinn.tre.for.tilgangskontroll.toggle", boolean.class, false);

    private ArbeidsgiverAltinnTilgangerKlient arbeidsgiverAltinnTilgangerKlient;

    // Kun for testing
    AltinnTilgangTjeneste(ArbeidsgiverAltinnTilgangerKlient arbeidsgiverAltinnTilgangerKlient) {
        this.arbeidsgiverAltinnTilgangerKlient = arbeidsgiverAltinnTilgangerKlient;
    }

    public AltinnTilgangTjeneste() {
        this.arbeidsgiverAltinnTilgangerKlient = ArbeidsgiverAltinnTilgangerKlient.instance();
    }

    public boolean manglerTilgangTilBedriften(String orgNr) {
        return !harTilgangTilBedriften(orgNr);
    }

    // TODO: Må ryddes opp etter Altinn 3 ressurs overgang i prod.
    public boolean harTilgangTilBedriften(String orgnr) {
        var altinnRessurserBrukerHarTilgangTilPerOrgnr = arbeidsgiverAltinnTilgangerKlient.hentTilganger().orgNrTilTilganger();

        if (altinnRessurserBrukerHarTilgangTilPerOrgnr == null || altinnRessurserBrukerHarTilgangTilPerOrgnr.isEmpty()
            || !altinnRessurserBrukerHarTilgangTilPerOrgnr.containsKey(orgnr)) {
            SECURE_LOG.info("ALTINN: Bruker har ikke tilgang til orgnr: {}", orgnr);
            return false;
        }

        var brukersTilgangerForOrgnr = altinnRessurserBrukerHarTilgangTilPerOrgnr.get(orgnr);

        var tilgangsbeslutningAltinn2 = brukersTilgangerForOrgnr.contains(ALTINN_TO_TJENESTE);
        var tilgangsbeslutningAltinn3 = brukersTilgangerForOrgnr.contains(ALTINN_TRE_INNTEKTSMELDING_RESSURS);

        if (tilgangsbeslutningAltinn2 != tilgangsbeslutningAltinn3) { // hvis tilgang er ulikt mellom Altinn 2 og Altinn 3, logg for avstemming.
            LOG.info("ALTINN: Tilgangsbeslutninger er ulike for bruker! Altinn 2: {}, Altinn 3: {}.", tilgangsbeslutningAltinn2, tilgangsbeslutningAltinn3);
            SECURE_LOG.info("ALTINN: Brukers tilganger for orgnr {}: {}", orgnr, brukersTilgangerForOrgnr);
            Metrics.counter(COUNTER_TILGANG_BEDRIFT, List.of(Tag.of(TAG_TILGANG_OK, "Nei"))).increment();
        } else {
            Metrics.counter(COUNTER_TILGANG_BEDRIFT, List.of(Tag.of(TAG_TILGANG_OK, "Ja"))).increment();
        }

        return BRUK_ALTINN_TRE_FOR_TILGANGSKONTROLL ? tilgangsbeslutningAltinn3 : tilgangsbeslutningAltinn2;
    }

    // TODO: Må ryddes opp etter Altinn 3 ressurs overgang i prod.
     public List<String> hentBedrifterArbeidsgiverHarTilgangTil() {
        var orgNrBrukerHarTilgangTilPerRessurs = arbeidsgiverAltinnTilgangerKlient.hentTilganger().tilgangTilOrgNr();

        var orgNrMedGittTilgangIAltinn2 = hentSortertListeMedOrgNrMedGittTilgang(orgNrBrukerHarTilgangTilPerRessurs, ALTINN_TO_TJENESTE);
        var orgNrMedGittTilgangIAltinn3 = hentSortertListeMedOrgNrMedGittTilgang(orgNrBrukerHarTilgangTilPerRessurs,
            ALTINN_TRE_INNTEKTSMELDING_RESSURS);

        if (!orgNrMedGittTilgangIAltinn2.equals(orgNrMedGittTilgangIAltinn3)) { // listene må være sortert for å kunne sammenlignes direkte.
            LOG.info("ALTINN: Uoverensstemmelse i lister over bedrifter bruker har tilgang til mellom Altinn 2 og Altinn 3.");
            SECURE_LOG.info("ALTINN: Bruker har tilgang til følgende bedrifter: Altinn2: {}, Altinn3: {}", orgNrMedGittTilgangIAltinn2, orgNrMedGittTilgangIAltinn3);
            Metrics.counter(COUNTER_HENT_BEDRIFTER, List.of(Tag.of(TAG_LIK, "Nei"))).increment();
        } else {
            Metrics.counter(COUNTER_HENT_BEDRIFTER, List.of(Tag.of(TAG_LIK, "Ja"))).increment();
        }

        return BRUK_ALTINN_TRE_FOR_TILGANGSKONTROLL ? orgNrMedGittTilgangIAltinn3 : orgNrMedGittTilgangIAltinn2;
    }

    private static List<String> hentSortertListeMedOrgNrMedGittTilgang(Map<String, List<String>> orgNrBrukerHarTilgangTilPerRessurs, String ressurs) {
        return orgNrBrukerHarTilgangTilPerRessurs.getOrDefault(ressurs, List.of()).stream().sorted().toList();
    }
}
