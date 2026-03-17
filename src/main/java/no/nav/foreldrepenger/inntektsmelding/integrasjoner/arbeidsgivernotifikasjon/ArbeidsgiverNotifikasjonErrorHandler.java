package no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import static java.util.stream.Collectors.joining;

import java.net.URI;
import java.util.List;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLError;

import no.nav.vedtak.exception.TekniskException;

class ArbeidsgiverNotifikasjonErrorHandler {

    ArbeidsgiverNotifikasjonErrorHandler() {
    }

    public static <T> T handleError(List<GraphQLError> errors, URI uri, String kode) {
        throw new TekniskException(kode, String.format("Feil %s mot %s", errors.stream().map(GraphQLError::getMessage).collect(joining(",")), uri));
    }

    public static void handleValidationError(String typename, String feilmelding, String aksjon) {
        throw new TekniskException("F-FAGER", String.format("Funksjonel feil ved %s: %s:%s", aksjon, typename, feilmelding));
    }
}
