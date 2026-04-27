package no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;

@Dependent
public class InntektsmeldingRepository {

    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingRepository.class);

    private EntityManager entityManager;

    public InntektsmeldingRepository() {
        // CDI
    }

    @Inject
    public InntektsmeldingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Long lagreInntektsmelding(InntektsmeldingEntitet inntektsmeldingEntitet) {
        entityManager.persist(inntektsmeldingEntitet);
        entityManager.flush();
        return inntektsmeldingEntitet.getId();
    }

    public List<InntektsmeldingEntitet> hentInntektsmeldingerSortertNyesteFørst(AktørIdEntitet aktørId, String arbeidsgiverIdent, LocalDate startDato, Ytelsetype ytelsetype) {
        var query = entityManager.createQuery(
                "FROM InntektsmeldingEntitet where aktørId = :brukerAktørId and ytelsetype = :ytelsetype and arbeidsgiverIdent = :arbeidsgiverIdent and startDato = :startDato order by opprettetTidspunkt desc",
                InntektsmeldingEntitet.class)
            .setParameter("brukerAktørId", aktørId)
            .setParameter("arbeidsgiverIdent", arbeidsgiverIdent)
            .setParameter("ytelsetype", ytelsetype)
            .setParameter("startDato", startDato);
        return query.getResultList();
    }

    public Optional<InntektsmeldingEntitet> finnInntektsmelding(UUID inntektsmeldingUuid) {
        var query = entityManager.createQuery(
                "FROM InntektsmeldingEntitet where uuid = :oppgittUuid",
                InntektsmeldingEntitet.class)
            .setParameter("oppgittUuid", inntektsmeldingUuid);
        return Optional.ofNullable(query.getSingleResultOrNull());
    }

    public InntektsmeldingEntitet hent(long inntektsmeldingId) {
        return entityManager.find(InntektsmeldingEntitet.class, inntektsmeldingId);
    }

    public List<InntektsmeldingEntitet> hentInntektsmeldingerFraFilter(String orgnr,
                                                                       AktørIdEntitet aktørId,
                                                                       Ytelsetype ytelseType,
                                                                       LocalDate fom,
                                                                       LocalDate tom) {
        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(InntektsmeldingEntitet.class);
        var root = cq.from(InntektsmeldingEntitet.class);

        var predicates = new ArrayList<Predicate>();

        predicates.add(cb.equal(root.get("arbeidsgiverIdent"), Objects.requireNonNull(orgnr)));
        if (aktørId != null) {
            predicates.add(cb.equal(root.get("aktørId"), aktørId));
        }
        if (ytelseType != null) {
            predicates.add(cb.equal(root.get("ytelsetype"), ytelseType));
        }
        if (fom != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("opprettetTidspunkt"), fom.atStartOfDay()));
        }
        if (tom != null) {
            predicates.add(cb.lessThan(root.get("opprettetTidspunkt"), tom.plusDays(1).atStartOfDay()));
        }
        cq.where(predicates.toArray(new Predicate[0]));

        cq.orderBy(cb.desc(root.get("opprettetTidspunkt")));
        var query = entityManager.createQuery(cq);
        query.setMaxResults(1001);
        var result = query.getResultList();
        if (result.size() == 1001) {
            LOG.warn("Hentet 1000 inntektsmeldinger for orgnr {}, men det finnes flere som ikke er hentet ut", orgnr);
            var redusertListe = new ArrayList<>(result);
            redusertListe.removeLast();
            return redusertListe;
        }
        return result;
    }
}
