package no.nav.familie.inntektsmelding.pip;

import java.util.List;

import jakarta.enterprise.context.Dependent;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnRettigheterProxyKlient;

@Dependent
public class AltinnTilgangTjeneste {

    private final AltinnRettigheterProxyKlient altinnKlient;

    AltinnTilgangTjeneste() {
        this(AltinnRettigheterProxyKlient.instance());
    }

    public AltinnTilgangTjeneste(AltinnRettigheterProxyKlient altinnKlient) {
        this.altinnKlient = altinnKlient;
    }

    public boolean harTilgangTilBedriften(String orgNr) {
        return altinnKlient.harTilgangTilBedriften(orgNr);
    }

    public boolean manglerTilgangTilBedriften(String orgNr) {
        return !harTilgangTilBedriften(orgNr);
    }

    public List<String> hentBedrifterArbeidsgiverHarTilgangTil() {
        return altinnKlient.hentBedrifterArbeidsgiverHarTilgangTil();
    }
}
