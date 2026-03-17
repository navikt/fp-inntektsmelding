package no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;

@Dependent
public class InntektsmeldingRepository {

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

    //TODO: Denne brukes kun til test - vurder fjerning
    public Optional<InntektsmeldingEntitet> hentSisteInntektsmelding(AktørIdEntitet aktørId, String arbeidsgiverIdent, LocalDate startDato, Ytelsetype ytelsetype) {
        return hentInntektsmeldingerSortertNyesteFørst(aktørId, arbeidsgiverIdent,  startDato, ytelsetype).stream().findFirst();
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
}
