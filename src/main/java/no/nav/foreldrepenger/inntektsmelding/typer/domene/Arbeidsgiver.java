package no.nav.foreldrepenger.inntektsmelding.typer.domene;

public record Arbeidsgiver(String orgnr) {

    public static Arbeidsgiver fra(String orgnr) {
        return new Arbeidsgiver(orgnr);
    }

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
}
