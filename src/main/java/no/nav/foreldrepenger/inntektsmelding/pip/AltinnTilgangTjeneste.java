package no.nav.foreldrepenger.inntektsmelding.pip;

import static no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.AltinnRessurser.ALTINN_TRE_INNTEKTSMELDING_RESSURS;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.ArbeidsgiverAltinnTilgangerKlient;

@ApplicationScoped
public class AltinnTilgangTjeneste {

    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");

    private final ArbeidsgiverAltinnTilgangerKlient arbeidsgiverAltinnTilgangerKlient;

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
        var altinnRessurserBrukerHarTilgangTilPerOrgnr = arbeidsgiverAltinnTilgangerKlient.hentTilganger().orgNrTilTilganger();

        if (altinnRessurserBrukerHarTilgangTilPerOrgnr == null || altinnRessurserBrukerHarTilgangTilPerOrgnr.isEmpty()
            || !altinnRessurserBrukerHarTilgangTilPerOrgnr.containsKey(orgnr)) {
            SECURE_LOG.info("ALTINN: Bruker har ikke tilgang til orgnr: {}", orgnr);
            return false;
        }

        var brukersTilgangerForOrgnr = altinnRessurserBrukerHarTilgangTilPerOrgnr.get(orgnr);

        return brukersTilgangerForOrgnr.contains(ALTINN_TRE_INNTEKTSMELDING_RESSURS);
    }

     public Set<String> hentBedrifterArbeidsgiverHarTilgangTil() {
        var orgNrBrukerHarTilgangTilPerRessurs = arbeidsgiverAltinnTilgangerKlient.hentTilganger().tilgangTilOrgNr();
        return hentOrgNrMedGittTilgang(orgNrBrukerHarTilgangTilPerRessurs,
            ALTINN_TRE_INNTEKTSMELDING_RESSURS);
    }

    private static Set<String> hentOrgNrMedGittTilgang(Map<String, List<String>> orgNrBrukerHarTilgangTilPerRessurs, String ressurs) {
        return orgNrBrukerHarTilgangTilPerRessurs.getOrDefault(ressurs, List.of())
            .stream()
            .map(String::strip)
            .collect(Collectors.toCollection(HashSet::new));
    }
}
