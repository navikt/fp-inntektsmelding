package no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record Kontaktperson(@Size(max = 200) @NotNull String navn, @NotNull @Size(max = 100) String telefonnummer) {
}
