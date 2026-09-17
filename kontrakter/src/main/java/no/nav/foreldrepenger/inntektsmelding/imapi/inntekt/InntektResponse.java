package no.nav.foreldrepenger.inntektsmelding.imapi.inntekt;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

public record InntektResponse(Map<YearMonth, BigDecimal> inntektPerMåned, BigDecimal gjennomsnitt) {
}
