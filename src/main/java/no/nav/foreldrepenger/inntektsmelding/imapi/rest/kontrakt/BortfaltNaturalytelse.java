package no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BortfaltNaturalytelse(@NotNull LocalDate fom,
                                    LocalDate tom,
                                    @NotNull Naturalytelsetype naturalytelsetype,
                                    @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
}
