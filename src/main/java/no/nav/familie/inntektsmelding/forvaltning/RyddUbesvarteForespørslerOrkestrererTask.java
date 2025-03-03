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
@ProsessTask(value = "rydd.ubesvarte.forespoersler.orkestrerer", maxFailedRuns = 1)
public class RyddUbesvarteForespørslerOrkestrererTask implements ProsessTaskHandler {
    static final String DRY_RUN = "dryRun";
    private static final Logger LOG = LoggerFactory.getLogger(RyddUbesvarteForespørslerOrkestrererTask.class);
    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    RyddUbesvarteForespørslerOrkestrererTask() {
        // for CDI proxy
    }

    @Inject
    public RyddUbesvarteForespørslerOrkestrererTask(EntityManager entityManager, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).map(Boolean::valueOf).orElse(Boolean.TRUE);

        var forespørsler = hentForespørsler();

        LOG.info("UBESVARTE_FORESPØRSLER: Fant {} ubesvarte forespørsler som ble opprettet før endring av inntektsmeldinglogikk i fpsak",
            forespørsler.size());

        forespørsler.forEach(forespørsel -> {
            LOG.info(
                "UBESVARTE_FORESPØRSLER: Forespørsel {} med oppgaveid {} for saksnummer {} med orgnummer {} og skjæringstidspunkt {} er kandidat for undersøkelse",
                forespørsel.getUuid(),
                Optional.ofNullable(forespørsel.getOppgaveId()),
                forespørsel.getFagsystemSaksnummer().orElse(null),
                forespørsel.getOrganisasjonsnummer(),
                forespørsel.getSkjæringstidspunkt().orElse(null));
            var task = ProsessTaskData.forTaskType(TaskType.forProsessTask(RyddUbesvartForespørselTask.class));
            task.setProperty(FORESPØRSEL_UUID, forespørsel.getUuid().toString());
            task.setProperty(DRY_RUN, dryRun.toString());
            prosessTaskTjeneste.lagre(task);

        });
    }

    private List<ForespørselEntitet> hentForespørsler() {
        var query = entityManager.createNativeQuery("""
                select * from forespoersel
                where opprettet_tid::date< '2025-02-12'
                  and status = 'UNDER_BEHANDLING'
            """, ForespørselEntitet.class);
        return query.getResultList();
    }
}

