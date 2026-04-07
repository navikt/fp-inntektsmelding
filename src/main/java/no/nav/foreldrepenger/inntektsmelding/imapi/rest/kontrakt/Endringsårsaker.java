package no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record Endringsårsaker(@NotNull @Valid Endringsårsak årsak,
                              LocalDate fom,
                              LocalDate tom,
                              LocalDate bleKjentFom) {
}
