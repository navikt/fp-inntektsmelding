package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/**
 * Midlertidig engangsjobb (master-task) som finner forespørsler med status {@link ForespørselStatus#UNDER_BEHANDLING}
 * og oppretter én {@link RyddForespørselTask} per (saksnummer, orgnummer)-kombinasjon. Selve
 * vurderingen mot fp-sak og lukkingen skjer i sub-tasken.
 * <p>
 * Paginerer {@value #MAKS_RADER_PER_TASK} rader om gangen. Sub-tasks for én side kjøres parallelt. Dersom siden
 * var full, opprettes en ny master-task for neste side, satt til å kjøre sekvensielt <b>etter</b> at hele
 * parallell-gruppen med sub-tasks for denne siden er ferdig - slik at vi ikke får ukontrollert mange samtidige
 * kall mot fp-sak på tvers av sider.
 */
@ApplicationScoped
@ProsessTask(value = "rydd.forespørsler", maxFailedRuns = 1)
public class RyddForespørslerTask implements ProsessTaskHandler {

    static final String DRY_RUN = "dryRun";
    static final String FRA_ID = "fraId";
    static final String TIL_ID = "tilId";

    /**
     * Antall rader hentet fra databasen per task.
     */
    private static final int MAKS_RADER_PER_TASK = 50;

    private static final String LOGG_PREFIKS = "RYDD_FORESPØRSEL";

    private static final Logger LOG = LoggerFactory.getLogger(RyddForespørslerTask.class);

    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    RyddForespørslerTask() {
        // for CDI proxy
    }

    @Inject
    public RyddForespørslerTask(EntityManager entityManager, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dryRun = !"false".equalsIgnoreCase(prosessTaskData.getPropertyValue(DRY_RUN));
        var fraId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_ID)).map(Long::valueOf).orElse(0L);
        var tilId = Optional.ofNullable(prosessTaskData.getPropertyValue(TIL_ID)).map(Long::valueOf);
        LOG.info("{}: Starter. Henter inntil {} forespørsler med id > {}{} (dryRun={}).",
            LOGG_PREFIKS, MAKS_RADER_PER_TASK, fraId, tilId.map(id -> " og id <= " + id).orElse(""), dryRun);

        var åpneForespørsler = hentÅpneForespørslerFraId(fraId, tilId);
        if (åpneForespørsler.isEmpty()) {
            LOG.info("{}: Ingen flere forespørsler med status UNDER_BEHANDLING funnet med id > {}. Jobben er ferdig.", LOGG_PREFIKS, fraId);
            return;
        }

        var taskGruppe = new ProsessTaskGruppe();
        taskGruppe.addNesteParallell(åpneForespørsler.stream()
            .map(f -> opprettSubTask(f.getFagsystemSaksnummer().orElseThrow(), f.getOrganisasjonsnummer(), dryRun))
            .toList());

        if (åpneForespørsler.size() == MAKS_RADER_PER_TASK) {
            // Fikk en full side, må anta at det finnes mer. Legges inn i samme gruppe, som sekvensiell etter
            // parallell-blokken over, slik at neste side ikke starter før alle sub-tasks i denne er ferdige.
            taskGruppe.addNesteSekvensiell(opprettNesteMasterTask(åpneForespørsler.getLast().getId(), tilId, dryRun));
        } else {
            LOG.info("{}: Siste side. Planlegger {} sub-task(er) og ingen flere master-tasks.", LOGG_PREFIKS, taskGruppe.getTasks().size());
        }

        prosessTaskTjeneste.lagre(taskGruppe);
    }

    private List<ForespørselEntitet> hentÅpneForespørslerFraId(long fraId, Optional<Long> tilId) {
        var queryString = "from ForespørselEntitet where id > :fraId"
            + (tilId.isPresent() ? " and id <= :tilId" : "")
            + " and status = :status order by id";
        var query = entityManager.createQuery(queryString, ForespørselEntitet.class);
        query.setParameter(FRA_ID, fraId);
        tilId.ifPresent(id -> query.setParameter(TIL_ID, id));
        query.setParameter("status", ForespørselStatus.UNDER_BEHANDLING);
        query.setMaxResults(MAKS_RADER_PER_TASK);
        return query.getResultList();
    }

    private static ProsessTaskData opprettSubTask(String saksnummer, String orgnummer, boolean dryRun) {
        var subTask = ProsessTaskData.forProsessTask(RyddForespørselTask.class);
        subTask.setProperty(RyddForespørselTask.SAKSNUMMER, saksnummer);
        subTask.setProperty(RyddForespørselTask.ORGNUMMER, orgnummer);
        subTask.setProperty(DRY_RUN, String.valueOf(dryRun));
        return subTask;
    }

    private static ProsessTaskData opprettNesteMasterTask(long nyFraId, Optional<Long> tilId, boolean dryRun) {
        var nesteTask = ProsessTaskData.forProsessTask(RyddForespørslerTask.class);
        nesteTask.setProperty(FRA_ID, String.valueOf(nyFraId));
        tilId.ifPresent(id -> nesteTask.setProperty(TIL_ID, String.valueOf(id)));
        nesteTask.setProperty(DRY_RUN, String.valueOf(dryRun));
        return nesteTask;
    }
}
