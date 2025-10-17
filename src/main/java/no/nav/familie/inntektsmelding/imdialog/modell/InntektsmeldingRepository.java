package no.nav.familie.inntektsmelding.imdialog.modell;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

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

    public Optional<InntektsmeldingEntitet> hentSisteInntektsmelding(AktørIdEntitet aktørId, String arbeidsgiverIdent, LocalDate startDato, Ytelsetype ytelsetype) {
        return hentInntektsmeldinger(aktørId, arbeidsgiverIdent,  startDato, ytelsetype).stream().findFirst();
    }

    public List<InntektsmeldingEntitet> hentInntektsmeldinger(AktørIdEntitet aktørId, String arbeidsgiverIdent, LocalDate startDato, Ytelsetype ytelsetype) {
        var query = entityManager.createQuery(
                "FROM InntektsmeldingEntitet where aktørId = :brukerAktørId and ytelsetype = :ytelsetype and arbeidsgiverIdent = :arbeidsgiverIdent and startDato = :startDato order by opprettetTidspunkt desc",
                InntektsmeldingEntitet.class)
            .setParameter("brukerAktørId", aktørId)
            .setParameter("arbeidsgiverIdent", arbeidsgiverIdent)
            .setParameter("ytelsetype", ytelsetype)
            .setParameter("startDato", startDato);

        return query.getResultList();
    }

    public Optional<InntektsmeldingEntitet> hentInntektsmelding(UUID uuid) {
        var query = entityManager.createQuery(
                "FROM InntektsmeldingEntitet where uuid = :oppgittUuid",
                InntektsmeldingEntitet.class)
            .setParameter("oppgittUuid", uuid);
        var res = query.getResultList();
        if (res.size() > 1) {
            throw new IllegalStateException(String.format("Forventet å finne kun en åpen inntektsmelding med uuid %s, men fant %s", uuid, res.size()));
        }
        return res.stream().findFirst();
    }

    public InntektsmeldingEntitet hentInntektsmelding(long inntektsmeldingId) {
        return entityManager.find(InntektsmeldingEntitet.class, inntektsmeldingId);
    }
}
