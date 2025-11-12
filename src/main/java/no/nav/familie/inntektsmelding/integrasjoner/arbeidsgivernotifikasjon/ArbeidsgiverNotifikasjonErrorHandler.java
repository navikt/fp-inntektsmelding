package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import static java.util.stream.Collectors.joining;

import java.net.URI;
import java.util.List;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLError;

import no.nav.vedtak.exception.TekniskException;

class ArbeidsgiverNotifikasjonErrorHandler {

    ArbeidsgiverNotifikasjonErrorHandler() {
    }

    public static <T> T handleError(List<GraphQLError> errors, URI uri, String kode) {
        throw new TekniskException(kode, "Feil %s mot %s".formatted(errors.stream().map(GraphQLError::getMessage).collect(joining(",")), uri));
    }

    public static void handleValidationError(String typename, String feilmelding, String aksjon) {
        throw new TekniskException("F-FAGER", "Funksjonel feil ved %s: %s:%s".formatted(aksjon, typename, feilmelding));
    }
}
