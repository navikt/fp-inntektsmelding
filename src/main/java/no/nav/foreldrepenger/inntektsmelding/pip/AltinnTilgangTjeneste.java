package no.nav.foreldrepenger.inntektsmelding.pip;

import static no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.AltinnRessurser.ALTINN_TO_TJENESTE;
import static no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.AltinnRessurser.ALTINN_TRE_INNTEKTSMELDING_RESSURS;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.ArbeidsgiverAltinnTilgangerKlient;
import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
public class AltinnTilgangTjeneste {

    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");
    private static final Logger LOG = LoggerFactory.getLogger(AltinnTilgangTjeneste.class);
    private static final Environment ENV = Environment.current();

    private final ArbeidsgiverAltinnTilgangerKlient arbeidsgiverAltinnTilgangerKlient;

    //TODO Etter migrering: Metrikker - kan vurderes fjerning etter migrering.
    private static final String APP_NAME = ENV.getNaisAppName().replace("-", "_");
    private static final String COUNTER_TILGANG_BEDRIFT = APP_NAME + "tilgang_til_org";
    private static final String COUNTER_HENT_BEDRIFTER = APP_NAME + "hent_org_liste";
    private static final String TAG_TILGANG_LIK = "tilgang_ok";
    private static final String TAG_SVAR_LIK = "lik_svar";

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

    public boolean harTilgangTilBedriften(String orgnr) {
        var altinnRessurserBrukerHarTilgangTilPerOrgnr = arbeidsgiverAltinnTilgangerKlient.hentTilganger(false).orgNrTilTilganger();

        if (altinnRessurserBrukerHarTilgangTilPerOrgnr == null || altinnRessurserBrukerHarTilgangTilPerOrgnr.isEmpty()
            || !altinnRessurserBrukerHarTilgangTilPerOrgnr.containsKey(orgnr)) {
            SECURE_LOG.info("ALTINN: Bruker har ikke tilgang til orgnr: {}", orgnr);
            return false;
        }

        var brukersTilgangerForOrgnr = altinnRessurserBrukerHarTilgangTilPerOrgnr.get(orgnr);

        var harTilgangGjennomAltinn3 = brukersTilgangerForOrgnr.contains(ALTINN_TRE_INNTEKTSMELDING_RESSURS);
        //TODO Etter migrering: Her kan mye kode ryddes og forenkles.
        var harTilgangGjennomAltinn2 = brukersTilgangerForOrgnr.contains(ALTINN_TO_TJENESTE);

        if (harTilgangGjennomAltinn2 != harTilgangGjennomAltinn3) {
            LOG.info("ALTINN: Tilgangsbeslutninger er ulike for bruker! Altinn 2: {}, Altinn 3: {}.", harTilgangGjennomAltinn2, harTilgangGjennomAltinn3);
            SECURE_LOG.info("ALTINN: Brukers tilganger for orgnr {}: {}", orgnr, brukersTilgangerForOrgnr);
            Metrics.counter(COUNTER_TILGANG_BEDRIFT, List.of(Tag.of(TAG_TILGANG_LIK, "Nei"))).increment();
            // TODO: denne her er kun for å kunne logge full response til brukeren - må fjernes etter man er ferdig med analyse.
            arbeidsgiverAltinnTilgangerKlient.hentTilganger(true);
        } else {
            Metrics.counter(COUNTER_TILGANG_BEDRIFT, List.of(Tag.of(TAG_TILGANG_LIK, "Ja"))).increment();
        }

        if (harTilgangGjennomAltinn3) {
            return true;
        }
        return harTilgangGjennomAltinn2;
    }

     public List<String> hentBedrifterArbeidsgiverHarTilgangTil() {
        var orgNrBrukerHarTilgangTilPerRessurs = arbeidsgiverAltinnTilgangerKlient.hentTilganger(false).tilgangTilOrgNr();

         //TODO Etter migrering: Her kan mye kode ryddes og forenkles.
        var orgNrMedGittTilgangIAltinn2 = hentOrgNrMedGittTilgang(orgNrBrukerHarTilgangTilPerRessurs, ALTINN_TO_TJENESTE);
        var orgNrMedGittTilgangIAltinn3 = hentOrgNrMedGittTilgang(orgNrBrukerHarTilgangTilPerRessurs,
            ALTINN_TRE_INNTEKTSMELDING_RESSURS);

        if (!orgNrMedGittTilgangIAltinn2.equals(orgNrMedGittTilgangIAltinn3)) {
            LOG.info("ALTINN: Uoverensstemmelse i lister over bedrifter bruker har tilgang til mellom Altinn 2 og Altinn 3.");
            SECURE_LOG.info("ALTINN: Bruker har tilgang til følgende bedrifter: Altinn2: {}, Altinn3: {}", orgNrMedGittTilgangIAltinn2, orgNrMedGittTilgangIAltinn3);
            Metrics.counter(COUNTER_HENT_BEDRIFTER, List.of(Tag.of(TAG_SVAR_LIK, "Nei"))).increment();
            // TODO: denne her er kun for å kunne logge full response til brukeren - må fjernes etter man er ferdig med analyse.
            arbeidsgiverAltinnTilgangerKlient.hentTilganger(true);
        } else {
            Metrics.counter(COUNTER_HENT_BEDRIFTER, List.of(Tag.of(TAG_SVAR_LIK, "Ja"))).increment();
        }

        if (!orgNrMedGittTilgangIAltinn3.containsAll(orgNrMedGittTilgangIAltinn2)) {
            orgNrMedGittTilgangIAltinn3.addAll(orgNrMedGittTilgangIAltinn2); // vi legger på det som mangler fra Altinn 2.
        }
        return orgNrMedGittTilgangIAltinn3.stream().toList();
    }

    private static Set<String> hentOrgNrMedGittTilgang(Map<String, List<String>> orgNrBrukerHarTilgangTilPerRessurs, String ressurs) {
        return new HashSet<>(orgNrBrukerHarTilgangTilPerRessurs.getOrDefault(ressurs, List.of()));
    }
}
