package no.nav.foreldrepenger.inntektsmelding.felles;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KontaktpersonDto(@Size(max = 200) @NotNull String navn, @NotNull @Size(max = 100) String telefonnummer) {
}
