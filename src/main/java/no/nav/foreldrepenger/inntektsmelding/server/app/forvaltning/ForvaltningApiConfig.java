package no.nav.foreldrepenger.inntektsmelding.server.app.forvaltning;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forvaltning.DialogportenForvaltningRestTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forvaltning.OppgaverForvaltningRestTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forvaltning.ProsessTaskRestTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forvaltning.rest.ForespørselVtpRest;
import no.nav.foreldrepenger.inntektsmelding.server.auth.AutentiseringAnnoteringFilter;
import no.nav.foreldrepenger.inntektsmelding.server.openapi.OpenApiRest;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.openapi.OpenApiUtils;
import no.nav.vedtak.server.rest.ForvaltningAuthorizationFilter;
import no.nav.vedtak.server.rest.FpRestJackson2Feature;

@ApplicationPath(ForvaltningApiConfig.API_URI)
public class ForvaltningApiConfig extends ResourceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningApiConfig.class);
    public static final String API_URI = "/forvaltning/api";
    private static final Environment ENV = Environment.current();

    public ForvaltningApiConfig() {
        LOG.info("Initialiserer: {}", API_URI);
        register(FpRestJackson2Feature.class);
        // Sikkerhet - lokal autentisering - kan antagelig saneres gitt import av abac/BeskyttetRessurs
        register(AutentiseringAnnoteringFilter.class);
        register(ForvaltningAuthorizationFilter.class);
        registerOpenApi();

        // REST
        registerClasses(getApplicationClasses());

        setProperties(getApplicationProperties());
        LOG.info("Ferdig med initialisering av {}", API_URI);
    }

    private void registerOpenApi() {
        var contextPath = ENV.getProperty("context.path", "/fpinntektsmelding");
        OpenApiUtils.setupOpenApi("FP-inntektsmelding - Forvaltning", contextPath, getApplicationClasses(), this);
        register(OpenApiRest.class);
    }

    private Set<Class<?>> getApplicationClasses() {
        var classes = new HashSet<Class<?>>();
        classes.add(ProsessTaskRestTjeneste.class);
        classes.add(OppgaverForvaltningRestTjeneste.class);
        if (!Environment.current().isProd() ) {
            // kun skal være tilgjengelig i dev for testing
            classes.add(DialogportenForvaltningRestTjeneste.class);
        }
        if (Environment.current().isLocal()) {
            classes.add(ForespørselVtpRest.class);
        }
        return classes;
    }

    private Map<String, Object> getApplicationProperties() {
        Map<String, Object> properties = new HashMap<>();
        // Ref Jersey doc
        properties.put(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        properties.put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
        return properties;
    }

}
