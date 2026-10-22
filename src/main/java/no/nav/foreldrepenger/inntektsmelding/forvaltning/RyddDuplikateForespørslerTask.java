package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/**
 * Midlertidig engangsjobb (master-task) som finner aktive forespørsler med duplikater for samme
 * (saksnummer, orgnummer), og oppretter én {@link RyddDuplikateForespørslerGruppeTask} per kombinasjon.
 * <p>
 * Paginerer {@value #MAKS_RADER_PER_TASK} rader om gangen. Sub-tasks for én side kjøres parallelt, og neste
 * master-task kjøres sekvensielt etter dem.
 */
@ApplicationScoped
@ProsessTask(value = "rydd.duplikate.forespørsler", maxFailedRuns = 1)
public class RyddDuplikateForespørslerTask implements ProsessTaskHandler {

    static final String DRY_RUN = "dryRun";
    static final String FRA_ID = "fraId";
    static final String TIL_ID = "tilId";

    private static final int MAKS_RADER_PER_TASK = 50;
    private static final String LOGG_PREFIKS = "RYDD_DUPLIKATE_FORESPØRSLER";
    private static final Logger LOG = LoggerFactory.getLogger(RyddDuplikateForespørslerTask.class);

    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    RyddDuplikateForespørslerTask() {
        // for CDI proxy
    }

    @Inject
    public RyddDuplikateForespørslerTask(EntityManager entityManager, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dryRun = !"false".equalsIgnoreCase(prosessTaskData.getPropertyValue(DRY_RUN));
        var fraId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_ID)).map(Long::valueOf).orElse(0L);
        var tilId = Optional.ofNullable(prosessTaskData.getPropertyValue(TIL_ID)).map(Long::valueOf);

        var kandidater = hentKandidater(fraId, tilId);
        if (kandidater.isEmpty()) {
            LOG.info("{}: Ingen flere kandidater med id > {}. Jobben er ferdig.", LOGG_PREFIKS, fraId);
            return;
        }

        var taskGruppe = new ProsessTaskGruppe();
        var besøkteGrupper = new HashSet<List<String>>();
        taskGruppe.addNesteParallell(kandidater.stream()
            .filter(f -> besøkteGrupper.add(List.of(f.getFagsystemSaksnummer().orElseThrow(), f.getOrganisasjonsnummer())))
            .map(f -> opprettSubTask(f.getFagsystemSaksnummer().orElseThrow(), f.getOrganisasjonsnummer(), dryRun))
            .toList());

        if (kandidater.size() == MAKS_RADER_PER_TASK) {
            taskGruppe.addNesteSekvensiell(opprettNesteMasterTask(kandidater.getLast().getId(), tilId, dryRun));
        }

        LOG.info("{}: Planlegger {} task(er) for id-intervall={}..{} (dryRun={}).", LOGG_PREFIKS,
            taskGruppe.getTasks().size(), kandidater.getFirst().getId(), kandidater.getLast().getId(), dryRun);
        prosessTaskTjeneste.lagre(taskGruppe);
    }

    private List<ForespørselEntitet> hentKandidater(long fraId, Optional<Long> tilId) {
        var queryString = "select f from ForespørselEntitet f where f.id > :fraId"
            + (tilId.isPresent() ? " and f.id <= :tilId" : "")
            + " and f.status in :statuser and f.fagsystemSaksnummer is not null"
            + " and exists (select peer.id from ForespørselEntitet peer"
            + " where peer.fagsystemSaksnummer = f.fagsystemSaksnummer"
            + " and peer.organisasjonsnummer = f.organisasjonsnummer"
            + " and peer.id <> f.id and peer.status in :statuser)"
            + " order by f.id";
        var query = entityManager.createQuery(queryString, ForespørselEntitet.class);
        query.setParameter(FRA_ID, fraId);
        tilId.ifPresent(id -> query.setParameter(TIL_ID, id));
        query.setParameter("statuser", RyddDuplikateForespørslerGruppeTask.AKTIVE_STATUSER);
        query.setMaxResults(MAKS_RADER_PER_TASK);
        return query.getResultList();
    }

    private static ProsessTaskData opprettSubTask(String saksnummer, String orgnummer, boolean dryRun) {
        var subTask = ProsessTaskData.forProsessTask(RyddDuplikateForespørslerGruppeTask.class);
        subTask.setProperty(RyddDuplikateForespørslerGruppeTask.SAKSNUMMER, saksnummer);
        subTask.setProperty(RyddDuplikateForespørslerGruppeTask.ORGNUMMER, orgnummer);
        subTask.setProperty(DRY_RUN, String.valueOf(dryRun));
        return subTask;
    }

    private static ProsessTaskData opprettNesteMasterTask(long nyFraId, Optional<Long> tilId, boolean dryRun) {
        var nesteTask = ProsessTaskData.forProsessTask(RyddDuplikateForespørslerTask.class);
        nesteTask.setProperty(FRA_ID, String.valueOf(nyFraId));
        tilId.ifPresent(id -> nesteTask.setProperty(TIL_ID, String.valueOf(id)));
        nesteTask.setProperty(DRY_RUN, String.valueOf(dryRun));
        return nesteTask;
    }
}
