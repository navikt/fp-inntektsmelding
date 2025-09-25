package no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;

public record Inntektsopplysninger(BigDecimal gjennomsnitt, String orgnummer, List<InntektMåned> måneder) {
    public record InntektMåned(BigDecimal beløp, YearMonth månedÅr, MånedslønnStatus status){}

    @Override
    public String toString() {
        return "Inntektsopplysninger{" +
            "gjennomsnitt=" + gjennomsnitt +
            ", orgnummer='" + maskerId(orgnummer) + '\'' +
            ", måneder=" + måneder +
            '}';
    }

    private String maskerId(String id) {
        if (id == null) {
            return "";
        }
        var length = id.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        return "*".repeat(length - 4) + id.substring(length - 4);
    }

    public static boolean erLik(Inntektsopplysninger a, Inntektsopplysninger b) {
        return Objects.equals(a.gjennomsnitt(), b.gjennomsnitt()) &&
            Objects.equals(a.orgnummer(), b.orgnummer()) &&
            a.måneder().size() == b.måneder().size()  &&  a.måneder().containsAll(b.måneder());
    }

}
