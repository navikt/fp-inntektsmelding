package no.nav.foreldrepenger.inntektsmelding.felles;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvsenderSystemDto(@NotNull @Size(min = 5, max = 255) String systemNavn,
                                @NotNull @Size(min = 2, max = 255) String systemVersjon) {
}
