package no.nav.familie.inntektsmelding.server.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;

public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    private static final Logger LOG = LoggerFactory.getLogger(JsonParseExceptionMapper.class);

    @Override
    public Response toResponse(JsonParseException exception) {
        var feil = "FIM-299955: JSON-parsing feil: %s".formatted(exception.getMessage());
        LOG.warn(feil);
        return Response.status(Response.Status.BAD_REQUEST).entity(new FeilDto(feil)).type(MediaType.APPLICATION_JSON).build();
    }
}
