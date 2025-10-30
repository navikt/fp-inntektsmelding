package no.nav.familie.inntektsmelding.imdialog.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.exception.TekniskException;

@Entity(name = "InntektsmeldingEntitet")
@Table(name = "INNTEKTSMELDING")
public class InntektsmeldingEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "bruker_aktoer_id", nullable = false, updatable = false)))
    private AktørIdEntitet aktørId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ytelse_type")
    private Ytelsetype ytelsetype;

    @Column(name = "arbeidsgiver_ident")
    private String arbeidsgiverIdent;

    @Column(name = "uuid")
    private UUID uuid;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "inntektsmelding")
    private KontaktpersonEntitet kontaktperson;

    @Column(name = "start_dato_permisjon", nullable = false)
    private LocalDate startDato;

    @Column(name = "maaned_inntekt", nullable = false)
    private BigDecimal månedInntekt;

    @Column(name = "maaned_refusjon")
    private BigDecimal månedRefusjon;

    @Column(name = "refusjon_opphoersdato")
    private LocalDate opphørsdatoRefusjon;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt = LocalDateTime.now();

    @Column(name = "opprettet_av", updatable = false)
    private String opprettetAv;

    @Enumerated(EnumType.STRING)
    @Column(name = "kildesystem", nullable = false, updatable = false)
    private Kildesystem kildesystem;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "inntektsmelding")
    private List<RefusjonsendringEntitet> refusjonsendringer = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "inntektsmelding")
    private List<BortaltNaturalytelseEntitet> borfalteNaturalYtelser = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "inntektsmelding")
    private List<EndringsårsakEntitet> endringsårsaker = new ArrayList<>();

    public InntektsmeldingEntitet() {
        // Hibernate
    }

    public Long getId() {
        return id;
    }

    public AktørIdEntitet getAktørId() {
        return aktørId;
    }

    public Ytelsetype getYtelsetype() {
        return ytelsetype;
    }

    public String getArbeidsgiverIdent() {
        return arbeidsgiverIdent;
    }

    public LocalDate getStartDato() {
        return startDato;
    }

    public BigDecimal getMånedInntekt() {
        return månedInntekt;
    }

    public List<BortaltNaturalytelseEntitet> getBorfalteNaturalYtelser() {
        return borfalteNaturalYtelser;
    }

    public KontaktpersonEntitet getKontaktperson() {
        return kontaktperson;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public BigDecimal getMånedRefusjon() {
        return månedRefusjon;
    }

    public LocalDate getOpphørsdatoRefusjon() {
        return opphørsdatoRefusjon;
    }

    public List<RefusjonsendringEntitet> getRefusjonsendringer() {
        return refusjonsendringer;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public Kildesystem getKildesystem() {
        return kildesystem;
    }

    public List<EndringsårsakEntitet> getEndringsårsaker() {
        return endringsårsaker;
    }

    // TODO Sett notNullable og fjern Optional. Denne er optional helt til etterpopulering av feltet er fullført
    public Optional<UUID> getUuid() {
        return Optional.ofNullable(uuid);
    }

    private void leggTilRefusjonsendring(RefusjonsendringEntitet refusjonsendringEntitet) {
        if (refusjonsendringer.stream().anyMatch(r -> r.getFom().equals(refusjonsendringEntitet.getFom()))) {
            throw new IllegalStateException("Det finnes allerede en refusjonsendring for denne datoen: " + refusjonsendringEntitet.getFom());
        }
        refusjonsendringEntitet.setInntektsmelding(this);
        refusjonsendringer.add(refusjonsendringEntitet);
    }

    private void leggTilEndringsårsak(EndringsårsakEntitet endringsårsakEntitet) {
        endringsårsakEntitet.setInntektsmelding(this);
        endringsårsaker.add(endringsårsakEntitet);
    }

    void leggTilBortfalteNaturalytelse(BortaltNaturalytelseEntitet bortfaltNaturalytelse) {
        bortfaltNaturalytelse.setInntektsmelding(this);
        if (bortfaltNaturalytelse.getMånedBeløp().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("Bortfalt naturalytelse på kr 0 " + bortfaltNaturalytelse);
        }
        borfalteNaturalYtelser.add(bortfaltNaturalytelse);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InntektsmeldingEntitet entitet = (InntektsmeldingEntitet) o;
        return Objects.equals(aktørId, entitet.aktørId) && ytelsetype == entitet.ytelsetype && Objects.equals(arbeidsgiverIdent,
            entitet.arbeidsgiverIdent) && Objects.equals(startDato, entitet.startDato) && Objects.equals(opprettetTidspunkt,
            entitet.opprettetTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId, ytelsetype, arbeidsgiverIdent, startDato, opprettetTidspunkt);
    }

    @Override
    public String toString() {
        return "InntektsmeldingEntitet{" + "id=" + id + ", aktørId=" + maskerId(aktørId.getAktørId()) + ", ytelsetype=" + ytelsetype
            + ", arbeidsgiverIdent='"
            + maskerId(arbeidsgiverIdent) + '\'' + ", startDato=" + startDato + ", månedInntekt=" + månedInntekt + ", opprettetTidspunkt="
            + opprettetTidspunkt
            + ", refusjonendringer=" + refusjonsendringer + ", endringAvInntektÅrsaker=" + endringsårsaker + ", bortfaltNaturalYtelser="
            + borfalteNaturalYtelser + '}';
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final InntektsmeldingEntitet kladd = new InntektsmeldingEntitet();

        public Builder medAktørId(AktørIdEntitet aktørId) {
            kladd.aktørId = aktørId;
            return this;
        }

        // Mulighet for å eksplisitt overstyre opprettet tidspunkt for bruk i test
        public Builder medOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
            kladd.opprettetTidspunkt = opprettetTidspunkt;
            return this;
        }

        public Builder medYtelsetype(Ytelsetype ytelsetype) {
            kladd.ytelsetype = ytelsetype;
            return this;
        }

        public Builder medArbeidsgiverIdent(String arbeidsgiverIdent) {
            kladd.arbeidsgiverIdent = arbeidsgiverIdent;
            return this;
        }

        public Builder medKontaktperson(KontaktpersonEntitet kontaktpersonEntitet) {
            kontaktpersonEntitet.setInntektsmelding(kladd);
            kladd.kontaktperson = kontaktpersonEntitet;
            return this;
        }

        public Builder medStartDato(LocalDate startDato) {
            kladd.startDato = startDato;
            return this;
        }

        public Builder medMånedInntekt(BigDecimal månedInntekt) {
            kladd.månedInntekt = månedInntekt;
            return this;
        }

        public Builder medMånedRefusjon(BigDecimal månedRefusjon) {
            kladd.månedRefusjon = månedRefusjon;
            return this;
        }

        public Builder medRefusjonOpphørsdato(LocalDate opphørsdato) {
            kladd.opphørsdatoRefusjon = opphørsdato;
            return this;
        }

        public Builder medRefusjonsendringer(List<RefusjonsendringEntitet> refusjonsPeriode) {
            refusjonsPeriode.forEach(kladd::leggTilRefusjonsendring);
            return this;
        }

        public Builder medBortfaltNaturalytelser(List<BortaltNaturalytelseEntitet> naturalYtelser) {
            naturalYtelser.forEach(kladd::leggTilBortfalteNaturalytelse);
            return this;
        }

        public Builder medEndringsårsaker(List<EndringsårsakEntitet> endringsårsaker) {
            endringsårsaker.forEach(kladd::leggTilEndringsårsak);
            return this;
        }

        public Builder medOpprettetAv(String opprettetAv) {
            kladd.opprettetAv = opprettetAv;
            return this;
        }

        public Builder medKildesystem(Kildesystem kildesystem) {
            kladd.kildesystem = kildesystem;
            return this;
        }

        public InntektsmeldingEntitet build() {
            Objects.requireNonNull(kladd.startDato, "startdato");
            Objects.requireNonNull(kladd.månedInntekt, "månedsinntekt");
            validerRefusjon();
            validerNaturalytelser();
            kladd.uuid = UUID.randomUUID();
            return kladd;
        }

        private void validerNaturalytelser() {
            if (kladd.getBorfalteNaturalYtelser().stream().anyMatch(bn -> bn.getPeriode().getFom().isBefore(kladd.getStartDato()))) {
                throw new TekniskException("FPINNTEKTSMELDING_NATURALYTELSE_1",
                    String.format("Det er oppgitt naturalytelser som bortfaller før startdato for inntektsmeldingen, ugyldig tilstand. Oppgitte naturalytelser var %s og oppgitt startdato var %s",
                        kladd.getBorfalteNaturalYtelser(), kladd.getStartDato()));
            }
        }

        private void validerRefusjon() {
            // Kan ikke oppgi refusjon uten opphørsdato (selv om denne er tidenes ende), og gir ikke mening å oppgi opphørsdato uten refusjon
            if (kladd.månedRefusjon != null || kladd.opphørsdatoRefusjon != null) {
                Objects.requireNonNull(kladd.månedRefusjon, "månedRefusjon");
                Objects.requireNonNull(kladd.opphørsdatoRefusjon, "opphørsdatoRefusjon");
            }
            if (!kladd.getRefusjonsendringer().isEmpty() && kladd.getMånedRefusjon() == null) {
                throw new TekniskException("FPINNTEKTSMELDING_REFUSJON_1",
                    String.format("Kan ikke ha refusjonsendringer når det ikke er oppgitt refusjon. Endringer var %s",
                        kladd.getRefusjonsendringer()));
            }
            if (kladd.getRefusjonsendringer().stream().anyMatch(r -> !r.getFom().isAfter(kladd.getStartDato()))) {
                throw new TekniskException("FPINNTEKTSMELDING_REFUSJON_2",
                    String.format(
                        "Kan ikke ha refusjonsendring som gjelder fra startdato eller før, ugyldig tilstand. Endringer var %s og startdato var %s",
                        kladd.getRefusjonsendringer(),
                        kladd.getStartDato()));
            }
            if (kladd.getOpphørsdatoRefusjon() != null && kladd.getRefusjonsendringer()
                .stream()
                .anyMatch(r -> r.getFom().isAfter(kladd.getOpphørsdatoRefusjon()))) {
                throw new TekniskException("FPINNTEKTSMELDING_REFUSJON_3",
                    String.format("Kan ikke ha refusjonsendring etter opphørsdato, ugyldig tilstand. Endringer var %s og opphøsdato var %s",
                        kladd.getRefusjonsendringer(),
                        kladd.getOpphørsdatoRefusjon()));
            }
        }

    }

}
