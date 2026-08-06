package no.nav.foreldrepenger.inntektsmelding.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.function.Function;

import jakarta.validation.Valid;
import jakarta.ws.rs.core.Context;

import org.junit.jupiter.api.Test;


class RestApiInputValideringAnnoteringTest extends RestApiTester {

    private Function<Method, String> printKlasseOgMetodeNavn = (method -> String.format("%s.%s", method.getDeclaringClass(), method.getName()));

    /**
     * IKKE ignorer eller fjern denne testen, den sørger for at inputvalidering er i orden for REST-grensesnittene
     * <p>
     * Kontakt Team Humle hvis du trenger hjelp til å endre koden din slik at den går igjennom her
     */
    @Test
    void alle_felter_i_objekter_som_brukes_som_inputDTO_skal_enten_ha_valideringsannotering_eller_være_av_godkjent_type() {
        for (Method method : finnAlleRestMetoder()) {
            for (int i = 0; i < method.getParameterCount(); i++) {
                Parameter parameter = method.getParameters()[i];
                if (parameter.getType().isEnum()) {
                    continue;
                }
                assertThat(method.getParameterTypes()[i].isAssignableFrom(String.class) && !parameter.isAnnotationPresent(
                    jakarta.validation.constraints.Pattern.class)).as(
                    "REST-metoder skal ikke har parameter som er String eller mer generelt uten at @Pattern brukes. Bruk DTO-er og valider. "
                        + printKlasseOgMetodeNavn.apply(method)).isFalse();
                assertThat(isRequiredAnnotationPresent(parameter)).as(
                        "Alle parameter for REST-metoder skal være annotert riktig med @Valid. Var ikke det for " + printKlasseOgMetodeNavn.apply(method))
                    .withFailMessage("Fant parametere som mangler @Valid annotation '" + parameter + "'")
                    .isTrue();
            }
        }
    }

    private boolean isRequiredAnnotationPresent(Parameter parameter) {
        if (parameter.getAnnotation(Context.class) != null) {
            return true;
        }
        // For parameteriserte typer (List, Set, etc.) skal @Valid ligge inni <>, ikke på utsiden
        AnnotatedType annotatedType = parameter.getAnnotatedType();
        if (annotatedType instanceof AnnotatedParameterizedType apt) {
            return Arrays.stream(apt.getAnnotatedActualTypeArguments())
                .anyMatch(typeArg -> typeArg.getAnnotation(Valid.class) != null);
        }
        return parameter.getAnnotation(Valid.class) != null;
    }

}
