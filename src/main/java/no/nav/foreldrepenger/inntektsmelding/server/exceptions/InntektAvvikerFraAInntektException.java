package no.nav.foreldrepenger.inntektsmelding.server.exceptions;

import java.net.HttpURLConnection;
import java.math.BigDecimal;

import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.VLLogLevel;

/**
 * Kastes når oppgitt månedsinntekt i en inntektsmelding avviker fra gjennomsnittlig inntekt i
 * A-inntekt, uten at det er oppgitt en endringsårsak som forklarer avviket.
 */
public class InntektAvvikerFraAInntektException extends FunksjonellException {

    private static final String FEILKODE = "INNTEKT_AVVIKER_FRA_AINNTEKT";

    public InntektAvvikerFraAInntektException(BigDecimal gjennomsnittligInntektFraAInntekt, BigDecimal oppgittMånedsinntekt) {
        super(FEILKODE, String.format(
            "Inntekt i inntektsmelding er ulik inntekt fra A-inntekt, og ingen endringsårsak er oppgitt. "
                + "Gjennomsnittlig inntekt fra A-inntekt: %s, oppgitt inntekt i inntektsmelding: %s",
            gjennomsnittligInntektFraAInntekt, oppgittMånedsinntekt));
    }

    @Override
    public String getFeilkode() {
        return FEILKODE;
    }

    @Override
    public int getStatusCode() {
        return HttpURLConnection.HTTP_BAD_REQUEST;
    }

    @Override
    public VLLogLevel getLogLevel() {
        return VLLogLevel.INFO;
    }
}
