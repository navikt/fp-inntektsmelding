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
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.InntektsmeldingDialogRest;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.aginitiert.ArbeidsgiverinitiertDialogRest;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.kvittering.PdfDokumentRest;
import no.nav.foreldrepenger.inntektsmelding.overstyring.rest.InntektsmeldingFpsakRest;
import no.nav.foreldrepenger.inntektsmelding.server.auth.AutentiseringAnnoteringFilter;
import no.nav.vedtak.server.rest.FpRestJackson2Feature;
import no.nav.vedtak.server.rest.RestSecureLogFeature;

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends ResourceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(ApiConfig.class);
    public static final String API_URI = "/api";

    public ApiConfig() {
        LOG.info("Initialiserer: {}", API_URI);
        register(FpRestJackson2Feature.class);
        // Sørger for logging av feil (validering og annet) til sikkerlogg
        register(RestSecureLogFeature.class);
        // Sikkerhet - lokal "tilleggsautentisering" sjekker match IdentType og Annotering
        register(AutentiseringAnnoteringFilter.class);

        // REST
        registerClasses(getApplicationClasses());

        setProperties(getApplicationProperties());
        LOG.info("Ferdig med initialisering av {}", API_URI);
    }

    private Set<Class<?>> getApplicationClasses() {
        return Set.of(ForespørselRest.class,
            InntektsmeldingDialogRest.class,
            InntektsmeldingFpsakRest.class,
            ArbeidsgiverinitiertDialogRest.class,
            PdfDokumentRest.class,
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
