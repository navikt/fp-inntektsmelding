package no.nav.familie.inntektsmelding.server.app.forvaltning;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.integration.GenericOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import no.nav.familie.inntektsmelding.forvaltning.DialogportenForvaltningRestTjeneste;
import no.nav.familie.inntektsmelding.forvaltning.OppgaverForvaltningRestTjeneste;
import no.nav.familie.inntektsmelding.forvaltning.ProsessTaskRestTjeneste;
import no.nav.familie.inntektsmelding.forvaltning.rest.ForespørselVtpRest;
import no.nav.familie.inntektsmelding.server.auth.AutentiseringFilter;
import no.nav.familie.inntektsmelding.server.exceptions.ConstraintViolationMapper;
import no.nav.familie.inntektsmelding.server.exceptions.GeneralRestExceptionMapper;
import no.nav.familie.inntektsmelding.server.exceptions.JsonMappingExceptionMapper;
import no.nav.familie.inntektsmelding.server.exceptions.JsonParseExceptionMapper;
import no.nav.familie.inntektsmelding.server.jackson.JacksonJsonConfig;
import no.nav.familie.inntektsmelding.server.openapi.OpenApiRest;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;

@ApplicationPath(ForvaltningApiConfig.API_URI)
public class ForvaltningApiConfig extends ResourceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningApiConfig.class);
    public static final String API_URI = "/forvaltning/api";
    private static final Environment ENV = Environment.current();

    public ForvaltningApiConfig() {
        LOG.info("Initialiserer: {}", API_URI);
        // Sikkerhet
        register(AutentiseringFilter.class);
        register(ForvaltningAuthorizationFilter.class);
        registerOpenApi();

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

    private void registerOpenApi() {
        var oas = new OpenAPI();
        var info = new Info().title(ENV.getNaisAppName())
            .version(Optional.ofNullable(ENV.imageName()).orElse("1.0"))
            .description("REST grensesnitt for fp-inntektsmelding.");

        oas.info(info).addServersItem(new Server().url(ENV.getProperty("context.path", "/fpinntektsmelding")));
        var oasConfig = new SwaggerConfiguration().openAPI(oas)
            .prettyPrint(true)
            .resourceClasses(getApplicationClasses().stream().map(Class::getName).collect(Collectors.toSet()));
        try {
            new GenericOpenApiContextBuilder<>().openApiConfiguration(oasConfig).buildContext(true).read();
        } catch (OpenApiConfigurationException e) {
            throw new TekniskException("OPEN-API", e.getMessage(), e);
        }

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
