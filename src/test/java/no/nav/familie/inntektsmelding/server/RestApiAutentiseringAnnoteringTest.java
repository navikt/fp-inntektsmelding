package no.nav.familie.inntektsmelding.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;

/**
 * Sjekker at alle REST endepunkt har definert autorisasjon konfigurasjon for Autentisering.
 * Alle REST tjenester må ha en @Autorisert annotering enten på klasse nivå eller på metode nivå.
 */
class RestApiAutentiseringAnnoteringTest {

    protected static final List<Class<? extends Annotation>> GYLDIGE_ANNOTERINGER = List.of(AutentisertMedAzure.class,
        AutentisertMedTokenX.class,
        UtenAutentisering.class);

    @ParameterizedTest
    @MethodSource("finnAlleRestMetoder")
    void test_at_alle_restmetoder_er_annotert_med_gyldig_annotering(Method restMethod) {
        assertThat(finnGyldigAnnotering(restMethod))
            .withFailMessage("Mangler @%s eller @%s -annotering på %s".formatted(
                AutentisertMedAzure.class.getSimpleName(),
                AutentisertMedTokenX.class.getSimpleName(),
                restMethod))
            .isNotNull();
    }

    private Annotation finnGyldigAnnotering(Method method) {
        return findAnnotation(method.getAnnotations())
            .or(() -> findAnnotation(method.getDeclaringClass().getAnnotations()))
            .orElse(null);
    }

    private Optional<Annotation> findAnnotation(Annotation[] annotations) {
        return Arrays.stream(annotations)
            .filter(a -> RestApiAutentiseringAnnoteringTest.GYLDIGE_ANNOTERINGER.contains(a.annotationType()))
            .findFirst();
    }

    private static Collection<Method> finnAlleRestMetoder() {
        return RestApiTester.finnAlleRestMetoder();
    }

}
