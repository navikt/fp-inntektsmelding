package no.nav.foreldrepenger.inntektsmelding.forespørsel.lager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;

@Dependent
public class ForespørselRepository {

    protected static final String AKTØR_ID = "aktørId";
    protected static final String YTELSE_TYPE = "ytelseType";
    protected static final String OPPRETTET_TIDSPUNKT = "opprettetTidspunkt";
    private EntityManager entityManager;
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselRepository.class);

    public ForespørselRepository() {
    }

    @Inject
    public ForespørselRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public UUID lagreForespørsel(ForespørselEntitet forespørselEntitet) {
        LOG.info("ForespørselRepository: lagrer forespørsel entitet: {}", forespørselEntitet);
        entityManager.persist(forespørselEntitet);
        entityManager.flush();
        return forespørselEntitet.getUuid();
    }

    public void oppdaterOppgaveId(UUID forespørselUUID, String oppgaveId) {
        var forespørselOpt = hentForespørsel(forespørselUUID);
        if (forespørselOpt.isPresent()) {
            var forespørsel = forespørselOpt.get();
            forespørsel.setOppgaveId(oppgaveId);
            entityManager.persist(forespørsel);
            entityManager.flush();
        }
    }

    public void oppdaterArbeidsgiverNotifikasjonSakId(UUID forespørselUUID, String arbeidsgiverNotifikasjonSakId) {
        var forespørselOpt = hentForespørsel(forespørselUUID);
        if (forespørselOpt.isPresent()) {
            var forespørsel = forespørselOpt.get();
            forespørsel.setArbeidsgiverNotifikasjonSakId(arbeidsgiverNotifikasjonSakId);
            entityManager.persist(forespørsel);
            entityManager.flush();
        }
    }

    public Optional<ForespørselEntitet> hentForespørsel(UUID foreldpørselUuid) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where uuid = :foresporselUUID", ForespørselEntitet.class)
            .setParameter("foresporselUUID", foreldpørselUuid);

        var resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return Optional.empty();
        } else if (resultList.size() > 1) {
            throw new IllegalStateException("Forventet å finne kun en forespørsel for oppgitt uuid " + foreldpørselUuid);
        } else {
            return Optional.of(resultList.getFirst());
        }
    }

    public void ferdigstillForespørsel(String arbeidsgiverNotifikasjonSakId) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where sakId = :sak_id", ForespørselEntitet.class)
            .setParameter("sak_id", arbeidsgiverNotifikasjonSakId);
        var resultList = query.getResultList();

        resultList.forEach(f -> {
            f.setStatus(ForespørselStatus.FERDIG);
            entityManager.persist(f);
        });
        entityManager.flush();
    }

    public void settForespørselTilUtgått(String arbeidsgiverNotifikasjonSakId) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where sakId = :sak_id", ForespørselEntitet.class)
            .setParameter("sak_id", arbeidsgiverNotifikasjonSakId);
        var resultList = query.getResultList();

        resultList.forEach(f -> {
            f.setStatus(ForespørselStatus.UTGÅTT);
            entityManager.persist(f);
        });
        entityManager.flush();
    }


    public List<ForespørselEntitet> hentForespørslerPåSak(String fagsakSaksnummer) {
        var query = entityManager.createQuery("FROM ForespørselEntitet f where fagsystemSaksnummer = :saksnr", ForespørselEntitet.class)
            .setParameter("saksnr", fagsakSaksnummer);
        return query.getResultList();
    }

    public Optional<ForespørselEntitet> finnGjeldendeForespørsel(AktørIdEntitet aktørId,
                                                                 Ytelsetype ytelsetype,
                                                                 LocalDate stp,
                                                                 String arbeidsgiverIdent,
                                                                 String fagsakSaksnummer,
                                                                 LocalDate førsteUttaksdato) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where status in(:fpStatuser) "
                    + "and aktørId = :brukerAktørId "
                    + "and fagsystemSaksnummer = :fagsakNr "
                    + "and organisasjonsnummer = :arbeidsgiverIdent "
                    + "and skjæringstidspunkt = :skjæringstidspunkt "
                    + "and førsteUttaksdato = :førsteUttaksdato "
                    + "and ytelseType = :ytelsetype",
                ForespørselEntitet.class)
            .setParameter("fpStatuser", Set.of(ForespørselStatus.UNDER_BEHANDLING, ForespørselStatus.FERDIG))
            .setParameter("brukerAktørId", aktørId)
            .setParameter("fagsakNr", fagsakSaksnummer)
            .setParameter("arbeidsgiverIdent", arbeidsgiverIdent)
            .setParameter("skjæringstidspunkt", stp)
            .setParameter("førsteUttaksdato", førsteUttaksdato)
            .setParameter("ytelsetype", ytelsetype);

        var resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return Optional.empty();
        } else if (resultList.size() > 1) {
            throw new IllegalStateException(
                "Forventet å finne kun en forespørsel for gitt sak: %s, orgnr: %s, skjæringstidspunkt: %s og første uttaksdato: %s".formatted(fagsakSaksnummer, arbeidsgiverIdent, stp, førsteUttaksdato));
        } else {
            return Optional.of(resultList.getFirst());
        }
    }

    public List<ForespørselEntitet> finnÅpenForespørsel(String fagsystemSaksnummer) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where status=:status " + "and fagsystemSaksnummer=:saksnummer",
                ForespørselEntitet.class)
            .setParameter("saksnummer", fagsystemSaksnummer)
            .setParameter("status", ForespørselStatus.UNDER_BEHANDLING);
        return query.getResultList();
    }

    public Optional<ForespørselEntitet> finnÅpenForespørsel(String fagsakSaksnummer,
                                                            String organisasjonsnummer) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where status = :fpStatus "
                    + "and fagsystemSaksnummer = :fagsakNr "
                    + "and organisasjonsnummer = :arbeidsgiverIdent ",
                ForespørselEntitet.class)
            .setParameter("fpStatus", ForespørselStatus.UNDER_BEHANDLING)
            .setParameter("fagsakNr", fagsakSaksnummer)
            .setParameter("arbeidsgiverIdent", organisasjonsnummer);

        var resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return Optional.empty();
        } else if (resultList.size() > 1) {
            throw new IllegalStateException(String.format("Forventet å finne kun en åpen forespørsel for gitt sak %s og orgnr %s", fagsakSaksnummer, organisasjonsnummer));
        } else {
            return Optional.of(resultList.getFirst());
        }
    }

    public List<ForespørselEntitet> finnForespørslerForAktørId(AktørIdEntitet aktørId, Ytelsetype ytelsetype) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where aktørId=:aktørId "
                    + "and status !=:utgått and ytelseType=:ytelseType",
                ForespørselEntitet.class)
            .setParameter(AKTØR_ID, aktørId)
            .setParameter("utgått", ForespørselStatus.UTGÅTT)
            .setParameter(YTELSE_TYPE, ytelsetype);
        return query.getResultList();
    }

    public List<ForespørselEntitet> finnForespørsler(AktørIdEntitet aktørId, Ytelsetype ytelsetype, String orgnr) {
        var query = entityManager.createQuery("FROM ForespørselEntitet where aktørId=:aktørId "
                    + "and ytelseType=:ytelseType and organisasjonsnummer=:orgnr",
                ForespørselEntitet.class)
            .setParameter(AKTØR_ID, aktørId)
            .setParameter(YTELSE_TYPE, ytelsetype)
            .setParameter("orgnr", orgnr);
        return query.getResultList();
    }

    public ForespørselEntitet oppdaterFørsteUttaksdato(UUID forespørselUuid, LocalDate førsteUttaksdato) {
        var forespørselOpt = hentForespørsel(forespørselUuid);
        if (forespørselOpt.isPresent()) {
            var forespørselEnitet = forespørselOpt.get();
            forespørselEnitet.setFørsteUttaksdato(førsteUttaksdato);
            entityManager.persist(forespørselEnitet);
            entityManager.flush();
            return forespørselEnitet;
        } else {
            LOG.info("Finner ikke forespørsel med id {}", forespørselUuid);
        }
        return null;
    }

    public void oppdaterDialogportenUuid(UUID forespørselUuid, UUID dialogportenUuid) {
        var forespørselOpt = hentForespørsel(forespørselUuid);
        if (forespørselOpt.isPresent()) {
            LOG.info("Oppdaterer forespørsel {} med dialogportenUuid {}", forespørselUuid, dialogportenUuid);
            var forespørsel = forespørselOpt.get();
            forespørsel.setDialogportenUuid(dialogportenUuid);
            entityManager.persist(forespørsel);
            entityManager.flush();
        } else {
            LOG.info("Finner ikke forespørsel med id {}", forespørselUuid);
        }
    }

    public List<ForespørselEntitet> hentForespørslerFraFilter(String orgnr,
                                                              AktørIdEntitet aktørId,
                                                              ForespørselStatus status,
                                                              Ytelsetype ytelseType,
                                                              LocalDate fom,
                                                              LocalDate tom) {
        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(ForespørselEntitet.class);
        var root = cq.from(ForespørselEntitet.class);

        var predicates = new ArrayList<Predicate>();

        predicates.add(cb.equal(root.get("organisasjonsnummer"), Objects.requireNonNull(orgnr)));
        if (aktørId != null) {
            predicates.add(cb.equal(root.get(AKTØR_ID), aktørId));
        }
        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        if (ytelseType != null) {
            predicates.add(cb.equal(root.get(YTELSE_TYPE), ytelseType));
        }
        if (fom != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get(OPPRETTET_TIDSPUNKT), fom.atStartOfDay()));
        }
        if (tom != null) {
            predicates.add(cb.lessThan(root.get(OPPRETTET_TIDSPUNKT), tom.plusDays(1).atStartOfDay()));
        }
        cq.where(predicates.toArray(new Predicate[0]));

        cq.orderBy(cb.asc(root.get(OPPRETTET_TIDSPUNKT)));
        var query = entityManager.createQuery(cq);
        query.setMaxResults(1001);
        var result = query.getResultList();
        if (result.size() == 1001) {
            LOG.warn("Hentet 1000 forespørsler for orgnr {}, men det finnes flere forespørsler som ikke er hentet ut", orgnr);
            // Wrapper i ny liste for å omgå Immutable list
            var redusertListe = new ArrayList<>(result);
            redusertListe.removeLast();
            return redusertListe;
        }
        return result;
    }
}
