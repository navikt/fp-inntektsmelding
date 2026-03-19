package no.nav.foreldrepenger.inntektsmelding.typer.domene;

import java.util.Objects;

public record Saksnummer(String saksnummer) {

    public static Saksnummer fra(String saksnummer) {
        return new Saksnummer(saksnummer);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (Saksnummer) obj;
        return Objects.equals(this.saksnummer, that.saksnummer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnummer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + saksnummer + ">";
    }
}
