package no.nav.foreldrepenger.inntektsmelding.imapi.inntekt;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record InntektResponse(@NotNull Map<YearMonth, BigDecimal> inntektPerMåned, @NotNull BigDecimal gjennomsnitt) {
}
