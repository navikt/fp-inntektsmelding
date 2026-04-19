package no.nav.foreldrepenger.inntektsmelding.server.exceptions;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.MDC;

import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.feil.Feilkode;
import no.nav.vedtak.server.rest.FeilUtils;

/**
 * Vi ønsker ikke eksponere detaljerte feilmeldinger frontend. Vi spesialbehandler tilgangsmangel, ellers får alle en generell melding om serverfeil.
 * Legger alltid ved callId så frontend kan vise denne og vi kan finne den igjen i loggene hvis arbeidsgiver melder den inn.
 */
public class LokalRestExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable feil) {
        // TODO - diskuter om det holder med kode - en FeilDto med nullable feilmelding
        // Frontend ser på status og kode og har egne tekster - bruker ikke backendtekst.
        FeilUtils.ensureCallId();
        try {
            if (feil instanceof ManglerTilgangException) {
                return FeilUtils.responseFra(Response.Status.FORBIDDEN, Feilkode.IKKE_TILGANG, "Mangler tilgang");
            }
            FeilUtils.loggFeil(feil);
            if (feil instanceof InntektsmeldingException) {
                return FeilUtils.responseFra(feil);
            }
            return FeilUtils.responseFra(Response.Status.INTERNAL_SERVER_ERROR, Feilkode.GENERELL, "Serverfeil");
        } finally {
            MDC.remove("prosess"); //$NON-NLS-1$
        }
    }

}
