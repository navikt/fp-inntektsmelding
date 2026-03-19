package no.nav.foreldrepenger.inntektsmelding.server.app.internal;

import jakarta.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.server.app.internal.rest.HealtCheckRest;
import no.nav.foreldrepenger.inntektsmelding.server.app.internal.rest.PrometheusRestService;

@ApplicationPath(InternalApiConfig.API_URI)
public class InternalApiConfig extends ResourceConfig {
    private static final Logger LOG = LoggerFactory.getLogger(InternalApiConfig.class);
    public static final String API_URI = "/internal";

    public InternalApiConfig() {
        LOG.info("Initialiserer: {}", API_URI);
        register(HealtCheckRest.class);
        register(PrometheusRestService.class);
        LOG.info("Ferdig med initialisering av {}", API_URI);
    }
}
