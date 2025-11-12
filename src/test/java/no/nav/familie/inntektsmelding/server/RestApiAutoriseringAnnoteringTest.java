package no.nav.familie.inntektsmelding.server;

import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sjekker at alle REST endepunkt har definert for Authorisering/Tilgangskontroll.
 * Alle REST tjenester må ha en @Tilgangskontrollert annotering på metode nivå.
 */
class RestApiAutoriseringAnnoteringTest {

    protected static final List<Class<? extends Annotation>> GYLDIGE_ANNOTERINGER = List.of(Tilgangskontrollert.class);
    protected static final String FEILMELDING = "Har du tenkt over tilgangskontroll? Husk å kalle på en av de sjekkene fra Tilgang grensesnittet før du kjører REST-logikk. Annoter metoden med @%s -annotering etterpå. Gjelder %s.";

    @ParameterizedTest
    @MethodSource("finnAlleRestMetoder")
    void test_at_alle_restmetoder_er_annotert_med_gyldig_annotering(Method restMethod) {
        assertThat(finnGyldigAnnotering(restMethod))
                .withFailMessage(FEILMELDING.formatted(restMethod, Tilgangskontrollert.class.getSimpleName()))
                .isNotNull();
    }

    private Annotation finnGyldigAnnotering(Method method) {
        return findAnnotation(method.getAnnotations()).orElse(null);
    }

    private Optional<Annotation> findAnnotation(Annotation[] annotations) {
        return Arrays.stream(annotations)
                .filter(a -> RestApiAutoriseringAnnoteringTest.GYLDIGE_ANNOTERINGER.contains(a.annotationType()))
                .findFirst();
    }

    private static Collection<Method> finnAlleRestMetoder() {
        return RestApiTester.finnAlleRestMetoder();
    }

}
