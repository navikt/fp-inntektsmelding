package no.nav.familie.inntektsmelding.server.app.api;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;

import no.nav.familie.inntektsmelding.imdialog.rest.ArbeidsgiverinitiertDialogRest;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.rest.ForespørselRest;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingDialogRest;
import no.nav.familie.inntektsmelding.overstyring.InntektsmeldingFpsakRest;
import no.nav.familie.inntektsmelding.server.auth.AutentiseringFilter;
import no.nav.familie.inntektsmelding.server.exceptions.ConstraintViolationMapper;
import no.nav.familie.inntektsmelding.server.exceptions.GeneralRestExceptionMapper;
import no.nav.familie.inntektsmelding.server.exceptions.JsonMappingExceptionMapper;
import no.nav.familie.inntektsmelding.server.exceptions.JsonParseExceptionMapper;
import no.nav.familie.inntektsmelding.server.jackson.JacksonJsonConfig;

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends ResourceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(ApiConfig.class);
    public static final String API_URI = "/api";

    public ApiConfig() {
        LOG.info("Initialiserer: {}", API_URI);
        // Sikkerhet
        register(AutentiseringFilter.class);

        // REST
        registerClasses(getApplicationClasses());

        registerExceptionMappers();
        register(JacksonJsonConfig.class);

        setProperties(getApplicationProperties());
        LOG.info("Ferdig med initialisering av {}", API_URI);
    }

    void registerExceptionMappers() {
        register(GeneralRestExceptionMapper.class);
        register(ConstraintViolationMapper.class);
        register(JsonMappingExceptionMapper.class);
        register(JsonParseExceptionMapper.class);
    }

    private Set<Class<?>> getApplicationClasses() {
        return Set.of(ForespørselRest.class,
            InntektsmeldingDialogRest.class,
            InntektsmeldingFpsakRest.class,
            ArbeidsgiverinitiertDialogRest.class);
    }

    private Map<String, Object> getApplicationProperties() {
        Map<String, Object> properties = new HashMap<>();
        // Ref Jersey doc
        properties.put(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        properties.put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
        return properties;
    }

}
