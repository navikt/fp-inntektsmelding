package no.nav.familie.inntektsmelding.pip;

import java.util.List;

import jakarta.enterprise.context.Dependent;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.ArbeidsgiverAltinnTilgangerKlient;
import no.nav.foreldrepenger.konfig.Environment;

@Dependent
public class AltinnTilgangTjeneste {
    private static final Environment ENV = Environment.current();

    private final ArbeidsgiverAltinnTilgangerKlient arbeidsgiverAltinnTilgangerKlient;

    private static final boolean BRUK_ALTINN_TRE_RESSURS = ENV.getProperty("bruk.altinn.tre.inntektsmelding.ressurs", boolean.class, false);

    AltinnTilgangTjeneste() {
        this(ArbeidsgiverAltinnTilgangerKlient.instance());
    }

    AltinnTilgangTjeneste(ArbeidsgiverAltinnTilgangerKlient arbeidsgiverAltinnTilgangerKlient) {
        this.arbeidsgiverAltinnTilgangerKlient = arbeidsgiverAltinnTilgangerKlient;
    }

    public boolean harTilgangTilBedriften(String orgNr) {
        return arbeidsgiverAltinnTilgangerKlient.harTilgangTilBedriften(orgNr, BRUK_ALTINN_TRE_RESSURS);
    }

    public boolean manglerTilgangTilBedriften(String orgNr) {
        return !harTilgangTilBedriften(orgNr);
    }

    public List<String> hentBedrifterArbeidsgiverHarTilgangTil() {
        return arbeidsgiverAltinnTilgangerKlient.hentBedrifterArbeidsgiverHarTilgangTil(BRUK_ALTINN_TRE_RESSURS);
    }
}
