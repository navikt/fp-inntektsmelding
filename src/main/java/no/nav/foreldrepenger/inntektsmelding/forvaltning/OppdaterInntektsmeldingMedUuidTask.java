package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.InntektsmeldingEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(value = "inntektsmelding.uuid", maxFailedRuns = 1)
public class OppdaterInntektsmeldingMedUuidTask implements ProsessTaskHandler {
    private static final String FRA_OG_MED = "fraOgMed";
    private static final String TIL_OG_MED = "tilOgMed";
    private static final Logger LOG = LoggerFactory.getLogger(OppdaterInntektsmeldingMedUuidTask.class);
    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    OppdaterInntektsmeldingMedUuidTask() {
        // for CDI proxy
    }

    @Inject
    public OppdaterInntektsmeldingMedUuidTask(EntityManager entityManager, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        LOG.info("Starter task inntektsmelding.uuid");
        var fraOgMedId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_OG_MED)).map(Long::valueOf).orElseThrow();
        var tilOgMedId = Optional.ofNullable(prosessTaskData.getPropertyValue(TIL_OG_MED)).map(Long::valueOf).orElseThrow();

        var inntektsmeldinger = hentInntektsmeldinger(fraOgMedId, tilOgMedId);
        inntektsmeldinger.forEach(im -> {
            LOG.info("Setter uuid for im id {}", im.getId());
            im.setUuid(UUID.randomUUID());
        });
        entityManager.flush();
        inntektsmeldinger.stream()
            .map(InntektsmeldingEntitet::getId)
            .max(Long::compareTo)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId + 1, tilOgMedId)));
        LOG.info("Avslutter task inntektsmelding.uuid");
    }

    private List<InntektsmeldingEntitet> hentInntektsmeldinger(long fom, long tom) {
        return entityManager.createQuery(
                "FROM InntektsmeldingEntitet WHERE id >= :fom AND id <= :tom AND uuid IS NULL ORDER BY id",
                InntektsmeldingEntitet.class)
            .setParameter("fom", fom)
            .setParameter("tom", tom)
            .setMaxResults(10)
            .getResultList();
    }

    public static ProsessTaskData opprettNesteTask(Long nyFraOgMed, Long tilOgMed) {
        var prosessTaskData = ProsessTaskData.forProsessTask(OppdaterInntektsmeldingMedUuidTask.class);
        prosessTaskData.setProperty(FRA_OG_MED, String.valueOf(nyFraOgMed));
        prosessTaskData.setProperty(TIL_OG_MED, String.valueOf(tilOgMed));
        return prosessTaskData;
    }

}

