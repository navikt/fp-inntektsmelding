package no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity(name = "LpsSystemInfoEntitet")
@Table(name = "LPS_SYSTEM_INFORMASJON")
public class LpsSystemInfoEntitet {

    @Id
    @OneToOne
    @JoinColumn(name = "inntektsmelding_id", nullable = false, updatable = false, unique = true)
    private InntektsmeldingEntitet inntektsmelding;

    @Column(name = "navn", length = 100, nullable = false, updatable = false)
    private String navn;

    @Column(name = "versjon", length = 100, nullable = false, updatable = false)
    private String versjon;

    public LpsSystemInfoEntitet() {
        // Hibernate
    }

    public LpsSystemInfoEntitet(String navn, String versjon) {
        this.versjon = versjon;
        this.navn = navn;
    }

    void setInntektsmelding(InntektsmeldingEntitet inntektsmeldingEntitet) {
        this.inntektsmelding = inntektsmeldingEntitet;
    }

    public InntektsmeldingEntitet getInntektsmelding() {
        return inntektsmelding;
    }

    public String getVersjon() {
        return versjon;
    }

    public String getNavn() {
        return navn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (LpsSystemInfoEntitet) o;
        return Objects.equals(navn, that.navn) && Objects.equals(versjon, that.versjon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(navn, versjon);
    }

    @Override
    public String toString() {
        return "LpsSystemEntitet{" + "navn='" + navn + '\'' + ", versjon='" + versjon + '\'' + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final LpsSystemInfoEntitet kladd = new LpsSystemInfoEntitet();

        public Builder medNavn(String navn) {
            kladd.navn = navn;
            return this;
        }

        public Builder medVersjon(String versjon) {
            kladd.versjon = versjon;
            return this;
        }

        public LpsSystemInfoEntitet build() {
            Objects.requireNonNull(kladd.navn, "navn");
            Objects.requireNonNull(kladd.versjon, "versjon");
            return kladd;
        }
    }
}
