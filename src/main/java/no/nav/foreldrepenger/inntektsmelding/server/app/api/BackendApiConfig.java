package no.nav.foreldrepenger.inntektsmelding.server.app.api;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.rest.ForespørselRest;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.forespørsel.ForespørselApiRest;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.inntektsmelding.InntektsmeldingApiRest;
import no.nav.foreldrepenger.inntektsmelding.overstyring.rest.InntektsmeldingFpsakRest;
import no.nav.foreldrepenger.inntektsmelding.server.auth.AutentiseringAnnoteringFilter;
import no.nav.vedtak.server.rest.FpRestJackson2Feature;

/**
 * For kall fra fpsak, fp-inntektsmelding-api, mv med kontekst systembruker eller internbruker
 * TODO: Skille rest-klassene fra frontend-api og legge på BeskyttetRessurs.
 */
@ApplicationPath(BackendApiConfig.API_URI)
public class BackendApiConfig extends ResourceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(BackendApiConfig.class);
    public static final String API_URI = "/backend/api";

    public BackendApiConfig() {
        LOG.info("Initialiserer: {}", API_URI);
        register(FpRestJackson2Feature.class);
        // Sikkerhet - lokal autentisering - kan antagelig saneres gitt import av abac/BeskyttetRessurs
        register(AutentiseringAnnoteringFilter.class);

        // REST
        registerClasses(getApplicationClasses());

        setProperties(getApplicationProperties());
        LOG.info("Ferdig med initialisering av {}", API_URI);
    }

    private Set<Class<?>> getApplicationClasses() {
        return Set.of(ForespørselRest.class,
            InntektsmeldingFpsakRest.class,
            ForespørselApiRest.class,
            InntektsmeldingApiRest.class);
    }

    private Map<String, Object> getApplicationProperties() {
        Map<String, Object> properties = new HashMap<>();
        // Ref Jersey doc
        properties.put(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        properties.put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
        return properties;
    }

}
