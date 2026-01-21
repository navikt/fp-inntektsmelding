package no.nav.familie.inntektsmelding.pip;

import java.util.List;

import jakarta.enterprise.context.Dependent;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.ArbeidsgiverAltinnTilgangerKlient;

@Dependent
public class AltinnTilgangTjeneste {

    private final ArbeidsgiverAltinnTilgangerKlient arbeidsgiverAltinnTilgangerKlient;

    AltinnTilgangTjeneste() {
        this(ArbeidsgiverAltinnTilgangerKlient.instance());
    }

    AltinnTilgangTjeneste(ArbeidsgiverAltinnTilgangerKlient arbeidsgiverAltinnTilgangerKlient) {
        this.arbeidsgiverAltinnTilgangerKlient = arbeidsgiverAltinnTilgangerKlient;
    }

    public boolean harTilgangTilBedriften(String orgNr) {
        return arbeidsgiverAltinnTilgangerKlient.harTilgangTilBedriften(orgNr);
    }

    public boolean manglerTilgangTilBedriften(String orgNr) {
        return !harTilgangTilBedriften(orgNr);
    }

    public List<String> hentBedrifterArbeidsgiverHarTilgangTil() {
        return arbeidsgiverAltinnTilgangerKlient.hentBedrifterArbeidsgiverHarTilgangTil();
    }
}
