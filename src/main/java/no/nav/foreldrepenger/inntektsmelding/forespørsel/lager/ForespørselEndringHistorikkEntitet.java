package no.nav.foreldrepenger.inntektsmelding.forespørsel.lager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@SequenceGenerator(name = "GLOBAL_PK_SEQ_GENERATOR", sequenceName = "SEQ_GLOBAL_PK")
@Entity(name = "ForespørselEndringHistorikkEntitet")
@Table(name = "FORESPOERSEL_ENDRING_HISTORIKK")
public class ForespørselEndringHistorikkEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "forespoersel_id")
    private ForespørselEntitet forespørsel;

    @Column(name = "skjaeringstidspunkt", updatable = false)
    private LocalDate skjæringstidspunkt;

    @Column(name = "forste_uttaksdato", updatable = false)
    private LocalDate førsteUttaksdato;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private final LocalDateTime opprettetTidspunkt = LocalDateTime.now();

    public ForespørselEndringHistorikkEntitet() {
        // Hibernate
    }

    public ForespørselEndringHistorikkEntitet(ForespørselEntitet forespørsel,
                                             LocalDate skjæringstidspunkt,
                                             LocalDate førsteUttaksdato) {
        if (førsteUttaksdato == null && skjæringstidspunkt == null) {
            throw new IllegalStateException("Må endre enten skjæringstidspunkt eller første uttaksdato");
        }
        this.forespørsel = Objects.requireNonNull(forespørsel, "forespørsel");
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.førsteUttaksdato = førsteUttaksdato;
    }

    public Long getId() {
        return id;
    }

    public ForespørselEntitet getForespørsel() {
        return forespørsel;
    }

    public Optional<LocalDate> getSkjæringstidspunkt() {
        return Optional.ofNullable(skjæringstidspunkt);
    }

    public Optional<LocalDate> getFørsteUttaksdato() {
        return Optional.ofNullable(førsteUttaksdato);
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    @Override
    public String toString() {
        return "ForespørselEndringHistorikkEntitet{" +
            "id=" + id +
            ", skjæringstidspunkt=" + skjæringstidspunkt +
            ", førsteUttaksdato=" + førsteUttaksdato +
            ", opprettetTidspunkt=" + opprettetTidspunkt +
            '}';
    }
}
