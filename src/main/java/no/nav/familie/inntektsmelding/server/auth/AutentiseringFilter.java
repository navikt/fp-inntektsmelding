package no.nav.familie.inntektsmelding.server.auth;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.vedtak.sikkerhet.jaxrs.AuthenticationFilterDelegate;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AutentiseringFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = LoggerFactory.getLogger(AutentiseringFilter.class);
    private static final List<Class<? extends Annotation>> GYLDIGE_ANNOTERINGER = List.of(AutentisertMedAzure.class,
        AutentisertMedTokenX.class,
        UtenAutentisering.class);

    @Context
    private ResourceInfo resourceinfo;

    public AutentiseringFilter() {
        // Ingenting
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        AuthenticationFilterDelegate.fjernKontekst();
    }

    @Override
    public void filter(ContainerRequestContext req) {
        assertValidRequest(req);
    }

    void assertValidRequest(ContainerRequestContext req) {
        var method = getResourceinfo().getResourceMethod();
        LOG.trace("{} i klasse {}", method.getName(), method.getDeclaringClass());
        fjernKontekstHvisFinnes();
        AuthenticationFilterDelegate.validerSettKontekst(getResourceinfo(), req);
        assertValidAnnotation(method, req);
    }

    private void assertValidAnnotation(Method method, ContainerRequestContext req) {
        var annotation = getAnnotation(method);
        LOG.debug("Annotering på {} -> {}", method.getName(), annotation);
        if (annotation != null) {
            assertValidAnnotation(annotation, req);
        } else {
            throw new WebApplicationException("Mangler en gyldig annotering på %s.".formatted(method.getName()), Response.Status.FORBIDDEN);
        }
    }

    /**
     * Letter etter en gyldig annotering på methoden og så på klassen.
     * Annoteringen på methodenivå overstyrer annotering på klassenivå.
     *
     * @param method REST mothoden som kalles
     * @return funnet annotering.
     */
    private static Annotation getAnnotation(Method method) {
        return findAnnotation(method.getAnnotations()).or(() -> findAnnotation(method.getDeclaringClass().getAnnotations())).orElse(null);
    }

    private static Optional<Annotation> findAnnotation(Annotation[] annotations) {
        return Arrays.stream(annotations).filter(a -> GYLDIGE_ANNOTERINGER.contains(a.annotationType())).findFirst();
    }

    private void assertValidAnnotation(Annotation annotering, ContainerRequestContext req) {
        switch (annotering) {
            case UtenAutentisering ignored -> {
                LOG.warn("Åpen endepunkt '{}' uten autentisering.", req.getMethod());
                validerIkkeAutentisertKontekst();
            }
            case AutentisertMedAzure ignored -> validerInternKontekst();
            case AutentisertMedTokenX ignored -> validerBorgerKontekst();
            case null, default -> throw new WebApplicationException("Mangler en gyldig annotering", Response.Status.UNAUTHORIZED);
        }
    }

    private void validerIkkeAutentisertKontekst() {
        var kontekst = KontekstHolder.getKontekst();
        if (!kontekst.harKontekst() || kontekst.getIdentType() != null) {
            throw new WebApplicationException("Kan ikke kjøre uten autentisering med autentisert kontekst", Response.Status.UNAUTHORIZED);
        }
    }

    private void validerInternKontekst() {
        var kontekst = KontekstHolder.getKontekst();
        if (!kontekst.harKontekst() || !Set.of(IdentType.InternBruker, IdentType.Systemressurs).contains(kontekst.getIdentType())) {
            throw new WebApplicationException("Ikke en gyldig intern eller system kontekst", Response.Status.UNAUTHORIZED);
        }
    }

    private void validerBorgerKontekst() {
        var kontekst = KontekstHolder.getKontekst();
        if (!kontekst.harKontekst() || !IdentType.EksternBruker.equals(kontekst.getIdentType())) {
            throw new WebApplicationException("Mangler gyldig borger kontekst", Response.Status.UNAUTHORIZED);
        }
    }

    private void fjernKontekstHvisFinnes() {
        if (KontekstHolder.harKontekst()) {
            LOG.info("Kall til {} hadde kontekst {}", getResourceinfo().getResourceMethod().getName(), KontekstHolder.getKontekst().getKompaktUid());
            KontekstHolder.fjernKontekst();
            MDC.clear();
        }
    }

    private ResourceInfo getResourceinfo() {
        return resourceinfo;
    }
}
