package no.nav.foreldrepenger.inntektsmelding.felles;

import jakarta.validation.constraints.NotNull;

public record AvsenderSystemDto(
    @NotNull String systemNavn,
    @NotNull String systemVersjon
) {
}
