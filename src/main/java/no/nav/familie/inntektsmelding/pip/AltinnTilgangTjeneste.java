package no.nav.familie.inntektsmelding.pip;

import java.util.List;

import jakarta.enterprise.context.Dependent;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnRettigheterProxyKlient;
import no.nav.familie.inntektsmelding.integrasjoner.altinn.ArbeidsgiverAltinnTilgangerKlient;

import no.nav.foreldrepenger.konfig.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class AltinnTilgangTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(AltinnTilgangTjeneste.class);
    private static final Environment ENV = Environment.current();

    private final AltinnRettigheterProxyKlient altinnRettigheterProxyKlient;
    private final ArbeidsgiverAltinnTilgangerKlient arbeidsgiverAltinnTilgangerKlient;

    private static final boolean BRUK_ALTINN_TRE_RESSURS = ENV.getProperty("bruk.altinn.tre.inntektsmelding.ressurs", boolean.class, false);

    AltinnTilgangTjeneste() {
        this(AltinnRettigheterProxyKlient.instance(), ArbeidsgiverAltinnTilgangerKlient.instance());
    }

    public AltinnTilgangTjeneste(AltinnRettigheterProxyKlient altinnRettigheterProxyKlient,
                                 ArbeidsgiverAltinnTilgangerKlient arbeidsgiverAltinnTilgangerKlient) {
        this.altinnRettigheterProxyKlient = altinnRettigheterProxyKlient;
        this.arbeidsgiverAltinnTilgangerKlient = arbeidsgiverAltinnTilgangerKlient;
    }

    public boolean harTilgangTilBedriften(String orgNr) {
        var proxyTilgang = altinnRettigheterProxyKlient.harTilgangTilBedriften(orgNr);
        if (ENV.isDev()) {
            try {
                var arbeidsgiverHarTilgang = arbeidsgiverAltinnTilgangerKlient.harTilgangTilBedriften(orgNr, BRUK_ALTINN_TRE_RESSURS);
                if (proxyTilgang != arbeidsgiverHarTilgang) {
                    LOG.info("ALTINN: Svar fra ny og gammel tilgangsklient er ulikt for orgNr: {}. Proxy: {}, ArbeidsgiverAltinnTilgangerKlient: {}",
                             orgNr, proxyTilgang, arbeidsgiverHarTilgang);
                }
            } catch (Exception e) {
                LOG.info("ALTINN: Feil ved kall til ny tilgang klient: {}", e.getMessage());
            }
        }
        return proxyTilgang;
    }

    public boolean manglerTilgangTilBedriften(String orgNr) {
        return !harTilgangTilBedriften(orgNr);
    }

    public List<String> hentBedrifterArbeidsgiverHarTilgangTil() {
        var harTilgangTil = altinnRettigheterProxyKlient.hentBedrifterArbeidsgiverHarTilgangTil();
        if (ENV.isDev()) {
            try {
                var arbeidsgiverHarTilgang = arbeidsgiverAltinnTilgangerKlient.hentBedrifterArbeidsgiverHarTilgangTil(BRUK_ALTINN_TRE_RESSURS);
                if (harTilgangTil != arbeidsgiverHarTilgang) {
                    LOG.info("ALTINN: Svar fra ny og gammel tilgangsklient er ulikt. Proxy: {}, ArbeidsgiverAltinnTilgangerKlient: {}",
                        harTilgangTil, arbeidsgiverHarTilgang);
                }
            } catch (Exception e) {
                LOG.info("ALTINN: Feil ved kall til ny tilgang klient: {}", e.getMessage());
            }
        }
        return harTilgangTil;
    }
}
