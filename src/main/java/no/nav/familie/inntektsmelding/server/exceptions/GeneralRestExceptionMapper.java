package no.nav.familie.inntektsmelding.server.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import no.nav.vedtak.exception.FunksjonellException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.log.util.LoggerUtils;

/**
 * Vi ønsker ikke eksponere detaljerte feilmeldinger frontend. Vi spesialbehandler tilgangsmangel, ellers får alle en generell melding om serverfeil.
 * Legger alltid ved callId så frontend kan vise denne og vi kan finne den igjen i loggene hvis arbeidsgiver melder den inn.
 */
@Provider
public class GeneralRestExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = LoggerFactory.getLogger(GeneralRestExceptionMapper.class);

    @Override
    public Response toResponse(Throwable feil) {
        try {
            if (feil instanceof ManglerTilgangException) {
                var exceptionMelding = getExceptionMelding(feil);
                LOG.warn("Tilgangsfeil: {}", exceptionMelding);
                return ikkeTilgang("Mangler tilgang");
            }
            if (feil instanceof FunksjonellException) {
                var exceptionMelding = getExceptionMelding(feil);
                if (exceptionMelding.contains("INGEN_SAK_FUNNET")) {
                    LOG.info("Ingen sak funnet feil: {}", exceptionMelding);
                    return ingenSakFunnet();
                }
            }
            if (feil instanceof FunksjonellException) {
                var exceptionMelding = getExceptionMelding(feil);
                if (exceptionMelding.contains("SENDT_FOR_TIDLIG")) {
                    LOG.info("Inntektsmelding sendt for tidlig feil: {}", exceptionMelding);
                    return sendtInnForTidlig();
                }
            }
            if (feil instanceof FunksjonellException) {
                var exceptionMelding = getExceptionMelding(feil);
                if (exceptionMelding.contains("FINNES_I_AAREG")) {
                    LOG.info("Organisasjonsnummer har rapportering i aa-reg feil: {}", exceptionMelding);
                    return finnesIAareg();
                }
            }
            loggTilApplikasjonslogg(feil);
            return serverError("Serverfeil");
        } finally {
            MDC.remove("prosess"); //$NON-NLS-1$
        }
    }

    private static Response serverError(String feilmelding) {
        return Response.serverError().entity(new FeilDto(FeilType.GENERELL_FEIL, feilmelding, MDCOperations.getCallId())).type(MediaType.APPLICATION_JSON).build();
    }

    private static Response ikkeTilgang(String feilmelding) {
        return Response.status(Response.Status.FORBIDDEN)
            .entity(new FeilDto(FeilType.MANGLER_TILGANG_FEIL, feilmelding, MDCOperations.getCallId()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static Response ingenSakFunnet() {
        return Response.status(Response.Status.FORBIDDEN)
            .entity(new FeilDto(FeilType.INGEN_SAK_FUNNET, "Ingen sak funnet", MDCOperations.getCallId()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static Response sendtInnForTidlig() {
        return Response.status(Response.Status.FORBIDDEN)
            .entity(new FeilDto(FeilType.SENDT_FOR_TIDLIG, "Sendt inntektsmelding for tidlig", MDCOperations.getCallId()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static Response finnesIAareg() {
        return Response.status(Response.Status.FORBIDDEN)
            .entity(new FeilDto(FeilType.FINNES_I_AAREG, "Organisasjonsnummer er rapportert i Aa-reg", MDCOperations.getCallId()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static void loggTilApplikasjonslogg(Throwable feil) {
        var melding = "Fikk uventet feil: " + getExceptionMelding(feil);
        LOG.warn(melding, feil);
    }

    private static String getExceptionMelding(Throwable feil) {
        return getTextForField(feil.getMessage());
    }

    private static String getTextForField(String input) {
        return input != null ? LoggerUtils.removeLineBreaks(input) : "";
    }
}
