package no.nav.foreldrepenger.inntektsmelding.felles;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EndringsårsakerDto(@NotNull @Valid EndringsårsakDto årsak,
                                 LocalDate fom,
                                 LocalDate tom,
                                 LocalDate bleKjentFom) {
}
