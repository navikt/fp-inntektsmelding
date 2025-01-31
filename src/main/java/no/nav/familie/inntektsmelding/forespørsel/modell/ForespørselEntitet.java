package no.nav.familie.inntektsmelding.forespørsel.modell;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@SequenceGenerator(name = "GLOBAL_PK_SEQ_GENERATOR", sequenceName = "SEQ_GLOBAL_PK")
@Entity(name = "ForespørselEntitet")
@Table(name = "FORESPOERSEL")
public class ForespørselEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Column(name = "sak_id")
    private String sakId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ForespørselStatus status = ForespørselStatus.UNDER_BEHANDLING;

    @Column(name = "oppgave_id")
    private String oppgaveId;

    @Column(name = "orgnr", nullable = false, updatable = false)
    private String organisasjonsnummer;

    @Column(name = "skjaeringstidspunkt", updatable = false)
    private LocalDate skjæringstidspunkt;

    @Column(name = "forste_uttaksdato", updatable = false)
    private LocalDate førsteUttaksdato;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "bruker_aktoer_id", nullable = false, updatable = false)))
    private AktørIdEntitet aktørId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ytelse_type", nullable = false, updatable = false)
    private Ytelsetype ytelseType;

    @Column(name = "fagsystem_saksnummer", updatable = false)
    private String fagsystemSaksnummer;

    @Column(name = "forespoersel_type", nullable = false, updatable = false)
    private ForespørselType forespørselType;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private final LocalDateTime opprettetTidspunkt = LocalDateTime.now();

    @Column(name = "endret_tid")
    private LocalDateTime endretTidspunkt;

    public ForespørselEntitet(String organisasjonsnummer,
                              LocalDate skjæringstidspunkt,
                              AktørIdEntitet aktørId,
                              Ytelsetype ytelseType,
                              String fagsystemSaksnummer,
                              LocalDate førsteUttaksdato,
                              ForespørselType forespørselType) {
        this.uuid = UUID.randomUUID();
        this.organisasjonsnummer = organisasjonsnummer;
        this.aktørId = aktørId;
        this.ytelseType = ytelseType;
        this.førsteUttaksdato = førsteUttaksdato;
        this.forespørselType = forespørselType;
        if (forespørselType.equals(ForespørselType.BESTILT_AV_FAGSYSTEM)) {
            this.fagsystemSaksnummer = Objects.requireNonNull(fagsystemSaksnummer, "fagsystemSaksnummer");
            this.skjæringstidspunkt = Objects.requireNonNull(skjæringstidspunkt, "skjæringstidspunkt");
        }
    }

    public ForespørselEntitet() {
        // Hibernate
    }

    @PreUpdate
    protected void onUpdate() {
        endretTidspunkt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getArbeidsgiverNotifikasjonSakId() {
        return sakId;
    }

    void setArbeidsgiverNotifikasjonSakId(String arbeidsgiverNotifikasjonSakId) {
        this.sakId = arbeidsgiverNotifikasjonSakId;
    }

    public ForespørselStatus getStatus() {
        return status;
    }

    public void setStatus(ForespørselStatus sakStatus) {
        this.status = sakStatus;
    }

    public Optional<String> getOppgaveId() {
        return Optional.ofNullable(oppgaveId);
    }

    void setOppgaveId(String oppgaveId) {
        this.oppgaveId = oppgaveId;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public AktørIdEntitet getAktørId() {
        return aktørId;
    }

    public Ytelsetype getYtelseType() {
        return ytelseType;
    }

    public String getFagsystemSaksnummer() {
        return fagsystemSaksnummer;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public Optional<LocalDate> getFørsteUttaksdato() {
        return Optional.ofNullable(førsteUttaksdato);
    }

    public ForespørselType getForespørselType() {
        return forespørselType;
    }

    @Override
    public String toString() {
        return "ForespørselEntitet{" + "id=" + id + ", uuid=" + uuid + ", sakId=" + sakId + ", organisasjonsnummer=" + maskerId(organisasjonsnummer)
            + ", skjæringstidspunkt=" + skjæringstidspunkt + ", forespørselType=" + forespørselType +
            ", aktørId=" + maskerId(aktørId.getAktørId()) + ", ytelseType=" + ytelseType
            + ", fagsystemSaksnummer=" + fagsystemSaksnummer + '}';
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
}
