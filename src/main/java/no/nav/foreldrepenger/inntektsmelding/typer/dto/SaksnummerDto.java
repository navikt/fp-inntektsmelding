package no.nav.foreldrepenger.inntektsmelding.typer.dto;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonValue;

public record SaksnummerDto(
    @JsonValue @NotNull @Pattern(regexp = REGEXP, message = "Saksnummer [${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String saksnr) {

    public static final String REGEXP = "^[\\p{Alnum}]+$";

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SaksnummerDto) obj;
        return Objects.equals(this.saksnr, that.saksnr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnr);
    }

    @Override
    public String toString() {
        return "FagsakSaksnummer[" + "saksnr=" + saksnr + ']';
    }
}
