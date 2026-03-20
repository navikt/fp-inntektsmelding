package no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonValue;

public record Arbeidsgiver(@JsonValue @NotNull @Digits(integer = 13, fraction = 0) @Pattern(regexp = REGEXP) String orgnr) {
    private static final String REGEXP = "^(\\d{9}|\\d{13})$";

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + maskerId() + ">";
    }

    private String maskerId() {
        if (orgnr == null) {
            return "";
        }
        var length = orgnr.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        return "*".repeat(length - 4) + orgnr.substring(length - 4);
    }

    public boolean erVirksomhet() {
        return orgnr.length() == 9;
    }

}
