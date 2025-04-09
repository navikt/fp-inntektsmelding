package no.nav.familie.inntektsmelding.typer.entitet;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AktørIdEntitet {
    private static final Pattern VALID = Pattern.compile("^\\d{13}");

    @Column(name = "aktoer_id")
    private String aktørId;

    public AktørIdEntitet() {
        // Hibernate
    }

    public AktørIdEntitet(String aktørId) {
        Objects.requireNonNull(aktørId, "aktørId");
        if (!VALID.matcher(aktørId).matches()) {
            // skal ikke skje, funksjonelle feilmeldinger håndteres ikke her.
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
        AktørIdEntitet that = (AktørIdEntitet) o;
        return Objects.equals(aktørId, that.aktørId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }

    private static final AtomicLong DUMMY_AKTØRID = new AtomicLong(1000000000000L);

    /**
     * Genererer dummy aktørid unikt for test.
     */
    public static AktørIdEntitet dummy() {
        return new AktørIdEntitet(String.valueOf(DUMMY_AKTØRID.getAndIncrement()));
    }
}
