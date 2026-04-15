package no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.EndringsårsakType;

@Entity(name = "EndringsårsakEntitet")
@Table(name = "ENDRINGSAARSAK")
public class EndringsårsakEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inntektsmelding_id", nullable = false, updatable = false)
    private InntektsmeldingEntitet inntektsmelding;

    @Enumerated(EnumType.STRING)
    @Column(name = "aarsak", nullable = false)
    private EndringsårsakType årsak;

    @Column(name = "fom")
    private LocalDate fom;

    @Column(name = "tom")
    private LocalDate tom;

    @Column(name = "ble_kjent_fom")
    private LocalDate bleKjentFom;

    public EndringsårsakEntitet() {
        // Hibernate
    }

    public EndringsårsakType getÅrsak() {
        return årsak;
    }

    public Optional<LocalDate> getFom() {
        return Optional.ofNullable(fom);
    }

    public Optional<LocalDate> getTom() {
        return Optional.ofNullable(tom);
    }

    public Optional<LocalDate> getBleKjentFom() {
        return Optional.ofNullable(bleKjentFom);
    }

    void setInntektsmelding(InntektsmeldingEntitet inntektsmelding) {
        this.inntektsmelding = inntektsmelding;
    }

    @Override
    public String toString() {
        return "EndringsårsakEntitet{" +
            "årsak=" + årsak +
            ", fom=" + fom +
            ", tom=" + tom +
            ", bleKjentFom=" + bleKjentFom +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final EndringsårsakEntitet kladd = new EndringsårsakEntitet();

        public Builder medÅrsak(EndringsårsakType årsak) {
            kladd.årsak = årsak;
            return this;
        }

        public Builder medFom(LocalDate fom) {
            kladd.fom = fom;
            return this;
        }

        public Builder medTom(LocalDate tom) {
            kladd.tom = tom;
            return this;
        }

        public Builder medBleKjentFra(LocalDate bleKjentFra) {
            kladd.bleKjentFom = bleKjentFra;
            return this;
        }

        public EndringsårsakEntitet build() {
            Objects.requireNonNull(kladd.årsak, "EndringsårsakType");
            return kladd;
        }
    }
}
