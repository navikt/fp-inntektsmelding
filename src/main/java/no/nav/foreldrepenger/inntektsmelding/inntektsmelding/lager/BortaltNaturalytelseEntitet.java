package no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;

@Entity(name = "BortfaltNaturalytelseEntitet")
@Table(name = "BORTFALT_NATURALYTELSE")
public class BortaltNaturalytelseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inntektsmelding_id", nullable = false, updatable = false)
    private InntektsmeldingEntitet inntektsmelding;

    @Embedded
    private PeriodeEntitet periode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NaturalytelseType type;

    @Column(name = "maaned_Beloep", nullable = false)
    private BigDecimal månedBeløp;

    public BortaltNaturalytelseEntitet() {
        // Hibernate
    }

    public PeriodeEntitet getPeriode() {
        return periode;
    }

    public NaturalytelseType getType() {
        return type;
    }

    public BigDecimal getMånedBeløp() {
        return månedBeløp;
    }

    void setInntektsmelding(InntektsmeldingEntitet inntektsmelding) {
        this.inntektsmelding = inntektsmelding;
    }

    @Override
    public String toString() {
        return "BortfaltNaturalytelseEntitet{" + "periode=" + periode + ", type=" + type + ", beløp=" + månedBeløp + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BortaltNaturalytelseEntitet kladd = new BortaltNaturalytelseEntitet();

        public Builder medPeriode(LocalDate fom, LocalDate tom) {
            kladd.periode = tom == null ? PeriodeEntitet.fraOgMed(fom) : PeriodeEntitet.fraOgMedTilOgMed(fom, tom);
            return this;
        }

        public Builder medMånedBeløp(BigDecimal beløp) {
            kladd.månedBeløp = beløp;
            return this;
        }

        public Builder medType(NaturalytelseType type) {
            kladd.type = type;
            return this;
        }

        public BortaltNaturalytelseEntitet build() {
            return kladd;
        }
    }
}
