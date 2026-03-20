package no.nav.foreldrepenger.inntektsmelding.felles;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonValue;

public record FødselsnummerDto(@JsonValue @NotNull @Pattern(regexp = "^\\d{11}$") @NotNull String fnr) {
    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + masker() + ">";
    }

    private String masker() {
        if (fnr == null) {
            return "";
        }
        var length = fnr.length();
        if (length <= 6) {
            return "*".repeat(length);
        }
        return fnr.substring(0, 6) + "*".repeat(length - 6);
    }
}
