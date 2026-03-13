package no.nav.familie.inntektsmelding.integrasjoner.person;

import java.util.Objects;
import java.util.regex.Pattern;

public class AktørId {

    private static final Pattern VALID = Pattern.compile("^\\d{13}");

    private String aktørId;

    public AktørId(String aktørId) {
        Objects.requireNonNull(aktørId, "aktørId");
        if (!VALID.matcher(aktørId).matches()) {
            throw new IllegalArgumentException("Ugyldig aktørId");
        }
        this.aktørId = aktørId;
    }

    public String getAktørId() {
        return aktørId;
    }

    @Override
    public String toString() {
        return "AktørIdEntitet{" + "aktørId='" + maskerId() + '\'' + '}';
    }

    private String maskerId() {
        if (aktørId == null) {
            return "";
        }
        var length = aktørId.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        return "*".repeat(length - 4) + aktørId.substring(length - 4);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (AktørId) o;
        return Objects.equals(aktørId, that.aktørId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }
}
