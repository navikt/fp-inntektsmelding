package no.nav.familie.inntektsmelding.server.exceptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import no.nav.vedtak.log.mdc.MDCOperations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOG = LoggerFactory.getLogger(ConstraintViolationMapper.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        log(exception);
        return lagResponse(exception);
    }

    private void log(ConstraintViolationException exception) {
        LOG.warn("Det oppstod en valideringsfeil: {}", constraints(exception));
        SECURE_LOG.warn("Det oppstod en valideringsfeil: felt {} - input {}", constraints(exception), getInputs(exception));
    }

    private static Response lagResponse(ConstraintViolationException exception) {
        Collection<FeltFeilDto> feilene = new ArrayList<>();
        for (var constraintViolation : exception.getConstraintViolations()) {
            var feltNavn = getFeltNavn(constraintViolation.getPropertyPath());
            feilene.add(new FeltFeilDto(feltNavn, constraintViolation.getMessage()));
        }
        var feltNavn = feilene.stream().map(FeltFeilDto::navn).toList();
        var feilmelding = String.format("Det oppstod en valideringsfeil på felt %s. " + "Vennligst kontroller at alle feltverdier er korrekte.",
            feltNavn);
        return Response.status(Response.Status.BAD_REQUEST).entity(new FeilDto(feilmelding, feilene, MDCOperations.getCallId())).type(MediaType.APPLICATION_JSON).build();
    }

    private static Set<String> getInputs(ConstraintViolationException exception) {
        return exception.getConstraintViolations()
            .stream()
            .map(ConstraintViolation::getInvalidValue)
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.toSet());
    }

    private static Set<String> constraints(ConstraintViolationException exception) {
        return exception.getConstraintViolations()
            .stream()
            .map(cv -> cv.getRootBeanClass().getSimpleName() + "." + cv.getLeafBean().getClass().getSimpleName() + "." + fieldName(cv) + " - "
                + cv.getMessage())
            .collect(Collectors.toSet());
    }

    private static String fieldName(ConstraintViolation<?> cv) {
        String field = null;
        for (var node : cv.getPropertyPath()) {
            field = node.getName();
        }
        return field;
    }

    private static String getFeltNavn(Path propertyPath) {
        return propertyPath instanceof org.hibernate.validator.path.Path path ? path.getLeafNode().toString() : null;
    }
}
