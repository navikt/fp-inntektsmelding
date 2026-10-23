package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import java.util.HashSet;
import java.util.List;

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
 * Midlertidig engangsjobb (master-task) som finner åpne forespørsler med duplikater for samme
 * (saksnummer, orgnummer), og oppretter én {@link RyddDuplikateForespørslerTask} per kombinasjon.
 * <p>
 * Paginerer {@value #MAKS_RADER_PER_TASK} rader om gangen. Sub-tasks for én side kjøres parallelt, og neste
 * master-task kjøres sekvensielt etter dem.
 */
@ApplicationScoped
@ProsessTask(value = "rydd.duplikate.åpne.forespørsler", maxFailedRuns = 1)
public class RyddDuplikateÅpneForespørslerTask implements ProsessTaskHandler {

    static final String DRY_RUN = "dryRun";
    static final String FRA_OG_MED_ID = "fraOgMedId";
    static final String TIL_OG_MED_ID = "tilOgMedId";

    private static final int MAKS_RADER_PER_TASK = 50;
    private static final String LOGG_PREFIKS = "RYDD_DUPLIKATE_FORESPØRSLER";
    private static final Logger LOG = LoggerFactory.getLogger(RyddDuplikateÅpneForespørslerTask.class);

    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    RyddDuplikateÅpneForespørslerTask() {
        // for CDI proxy
    }

    @Inject
    public RyddDuplikateÅpneForespørslerTask(EntityManager entityManager, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dryRun = !"false".equalsIgnoreCase(prosessTaskData.getPropertyValue(DRY_RUN));
        var fraOgMedId = påkrevdId(prosessTaskData, FRA_OG_MED_ID);
        var tilOgMedId = påkrevdId(prosessTaskData, TIL_OG_MED_ID);

        var kandidater = hentKandidater(fraOgMedId, tilOgMedId);
        if (kandidater.isEmpty()) {
            LOG.info("{}: Ingen flere kandidater med id >= {}. Jobben er ferdig.", LOGG_PREFIKS, fraOgMedId);
            return;
        }

        var taskPlan = new ProsessTaskGruppe();
        var besøkteSaksnummerOgOrgnr = new HashSet<List<String>>();
        taskPlan.addNesteParallell(kandidater.stream()
            .filter(f -> besøkteSaksnummerOgOrgnr.add(List.of(f.getFagsystemSaksnummer().orElseThrow(), f.getOrganisasjonsnummer())))
            .map(f -> opprettForespørselTask(f.getFagsystemSaksnummer().orElseThrow(), f.getOrganisasjonsnummer(), dryRun))
            .toList());

        var sisteId = kandidater.getLast().getId();
        if (kandidater.size() == MAKS_RADER_PER_TASK && sisteId < tilOgMedId) {
            taskPlan.addNesteSekvensiell(opprettNesteMasterTask(sisteId + 1, tilOgMedId, dryRun));
        }

        LOG.info("{}: Planlegger {} task(er) for id-intervall={}..{} (dryRun={}).", LOGG_PREFIKS,
            taskPlan.getTasks().size(), kandidater.getFirst().getId(), sisteId, dryRun);
        prosessTaskTjeneste.lagre(taskPlan);
    }

    private static long påkrevdId(ProsessTaskData prosessTaskData, String navn) {
        var verdi = prosessTaskData.getPropertyValue(navn);
        if (verdi == null) {
            throw new IllegalArgumentException("Mangler påkrevd property '" + navn + "'");
        }
        return Long.parseLong(verdi);
    }

    private List<ForespørselEntitet> hentKandidater(long fraOgMedId, long tilOgMedId) {
        var queryString = "select f from ForespørselEntitet f where f.id >= :fraOgMedId and f.id <= :tilOgMedId"
            + " and f.status in :statuser and f.fagsystemSaksnummer is not null"
            + " and exists (select peer.id from ForespørselEntitet peer"
            + " where peer.fagsystemSaksnummer = f.fagsystemSaksnummer"
            + " and peer.organisasjonsnummer = f.organisasjonsnummer"
            + " and peer.id <> f.id and peer.status in :statuser)"
            + " order by f.id";
        var query = entityManager.createQuery(queryString, ForespørselEntitet.class);
        query.setParameter(FRA_OG_MED_ID, fraOgMedId);
        query.setParameter(TIL_OG_MED_ID, tilOgMedId);
        query.setParameter("statuser", RyddDuplikateForespørslerTask.ÅPNE_STATUSER);
        query.setMaxResults(MAKS_RADER_PER_TASK);
        return query.getResultList();
    }

    private static ProsessTaskData opprettForespørselTask(String saksnummer, String orgnummer, boolean dryRun) {
        var forespørselTask = ProsessTaskData.forProsessTask(RyddDuplikateForespørslerTask.class);
        forespørselTask.setProperty(RyddDuplikateForespørslerTask.SAKSNUMMER, saksnummer);
        forespørselTask.setProperty(RyddDuplikateForespørslerTask.ORGNUMMER, orgnummer);
        forespørselTask.setProperty(DRY_RUN, String.valueOf(dryRun));
        return forespørselTask;
    }

    private static ProsessTaskData opprettNesteMasterTask(long nyFraOgMedId, long tilOgMedId, boolean dryRun) {
        var nesteTask = ProsessTaskData.forProsessTask(RyddDuplikateÅpneForespørslerTask.class);
        nesteTask.setProperty(FRA_OG_MED_ID, String.valueOf(nyFraOgMedId));
        nesteTask.setProperty(TIL_OG_MED_ID, String.valueOf(tilOgMedId));
        nesteTask.setProperty(DRY_RUN, String.valueOf(dryRun));
        return nesteTask;
    }
}
