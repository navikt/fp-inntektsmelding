package no.nav.foreldrepenger.inntektsmelding.typer.domene;

public record Saksnummer(String saksnummer) {

    public static Saksnummer fra(String saksnummer) {
        return new Saksnummer(saksnummer);
    }

    public boolean isNotEmpty() {
        return saksnummer != null && !saksnummer.isBlank();
    }
}
