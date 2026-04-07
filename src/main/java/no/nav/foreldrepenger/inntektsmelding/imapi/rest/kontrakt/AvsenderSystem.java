package no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AvsenderSystem(
    @NotNull @Min(value = 1) @Max(value = 100) String systemNavn,
    @NotNull @Min(value = 1) @Max(value = 100) String systemVersjon
) {
}
