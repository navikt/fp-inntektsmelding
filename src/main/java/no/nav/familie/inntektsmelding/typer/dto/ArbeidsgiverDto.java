package no.nav.familie.inntektsmelding.typer.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonValue;

public record ArbeidsgiverDto(@JsonValue @NotNull @Digits(integer = 13, fraction = 0) @Pattern(regexp = REGEXP) String ident) {
    private static final String REGEXP = "^(\\d{9}|\\d{13})$";

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + maskerId() + ">";
    }

    private String maskerId() {
        if (ident == null) {
            return "";
        }
        var length = ident.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        return "*".repeat(length - 4) + ident.substring(length - 4);
    }

    public boolean erVirksomhet() {
        return ident.length() == 9;
    }

}
