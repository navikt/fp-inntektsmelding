package no.nav.foreldrepenger.inntektsmelding.typer.domene;

public record Fødselsnummer(String fnr) {
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
