package no.nav.familie.inntektsmelding.forvaltning;

import static no.nav.familie.inntektsmelding.forvaltning.SlettForespørselTask.FORESPØRSEL_UUID;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ApplicationScoped
@ProsessTask(value = "slettForesporsler.feilaktig.opprettet.alle", maxFailedRuns = 1)
public class SlettFeilaktigeForespørslerAlleTask implements ProsessTaskHandler {
    static final String DRY_RUN = "dryRun";
    private static final Logger LOG = LoggerFactory.getLogger(SlettFeilaktigeForespørslerAlleTask.class);
    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    SlettFeilaktigeForespørslerAlleTask() {
        // for CDI proxy
    }

    @Inject
    public SlettFeilaktigeForespørslerAlleTask(EntityManager entityManager, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).map(Boolean::valueOf).orElse(Boolean.TRUE);

        var forespørsler = hentForespørsler();

        LOG.info("FEILAKTIGE_FORESPØRSLER: Fant {} forespørsler som skal slettes fordi de er opprettet for tidlig", forespørsler.size());

        forespørsler.forEach(forespørsel -> {
            LOG.info(
                "FEILAKTIGE_FORESPØRSLER: Forespørsel {} med oppgaveid {} for saksnummer {} med orgnummer {} og skjæringstidspunkt {} vil slettes",
                forespørsel.getUuid(),
                Optional.ofNullable(forespørsel.getOppgaveId()),
                forespørsel.getFagsystemSaksnummer(),
                forespørsel.getOrganisasjonsnummer(),
                forespørsel.getSkjæringstidspunkt());
            if (!dryRun) {
                var task = ProsessTaskData.forTaskType(TaskType.forProsessTask(SlettForespørselTask.class));
                task.setProperty(FORESPØRSEL_UUID, forespørsel.getUuid().toString());
                prosessTaskTjeneste.lagre(task);
            }
        });
    }

    private List<ForespørselEntitet> hentForespørsler() {
        var query = entityManager.createNativeQuery("""
                select * from forespoersel
                where opprettet_tid::date>= '2025-02-19'
                  and skjaeringstidspunkt >='2025-04-08'
                  and status = 'UNDER_BEHANDLING'
            """, ForespørselEntitet.class);

        return query.getResultList();
    }
}

