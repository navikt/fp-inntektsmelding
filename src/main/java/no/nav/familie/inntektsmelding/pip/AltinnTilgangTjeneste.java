package no.nav.familie.inntektsmelding.pip;

import jakarta.enterprise.context.Dependent;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnAutoriseringKlient;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;

import java.util.List;

@Dependent
public class AltinnTilgangTjeneste {

    private final AltinnAutoriseringKlient altinnKlient;

    public AltinnTilgangTjeneste() {
        this(AltinnAutoriseringKlient.instance());
    }

    public AltinnTilgangTjeneste(AltinnAutoriseringKlient altinnKlient) {
        this.altinnKlient = altinnKlient;
    }

    public boolean harTilgangTilBedriften(String orgNr) {
        return altinnKlient.harTilgangTilBedriften(orgNr);
    }

    public boolean manglerTilgangTilBedriften(String orgNr) {
        return !harTilgangTilBedriften(orgNr);
    }

    public List<String> hentBedrifterInnsenderHarTilgangTil() {
        return altinnKlient.hentBedrifterInnsenderHarTilgangTil();
    }
}
