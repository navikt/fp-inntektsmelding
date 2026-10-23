package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "rydd.duplikate.forespørsler", maxFailedRuns = 1)
public class RyddDuplikateForespørslerTask implements ProsessTaskHandler {

    static final String SAKSNUMMER = "saksnummer";
    static final String ORGNUMMER = "orgnummer";
    static final Set<ForespørselStatus> ÅPNE_STATUSER = Set.of(ForespørselStatus.UNDER_BEHANDLING, ForespørselStatus.FERDIG);

    private static final String LOGG_PREFIKS = "RYDD_DUPLIKATE_FORESPØRSLER";
    private static final Logger LOG = LoggerFactory.getLogger(RyddDuplikateForespørslerTask.class);

    private EntityManager entityManager;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    RyddDuplikateForespørslerTask() {
        // CDI
    }

    @Inject
    public RyddDuplikateForespørslerTask(EntityManager entityManager,
                                            ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.entityManager = entityManager;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData data) {
        var saksnummer = data.getPropertyValue(SAKSNUMMER);
        var orgnummer = data.getPropertyValue(ORGNUMMER);
        var dryRun = !"false".equalsIgnoreCase(data.getPropertyValue(RyddDuplikateÅpneForespørslerTask.DRY_RUN));
        var åpneForespørsler = hentForespørsler(saksnummer, orgnummer, dryRun);

        if (åpneForespørsler.size() < 2) {
            LOG.info("{}: saksnummer={}: Ingen åpne duplikater. dryRun={}", LOGG_PREFIKS, saksnummer, dryRun);
            return;
        }
        var forespørselSomBeholdes = finnForespørselSomBeholdes(åpneForespørsler);
        LOG.info("{}: BEHOLD id={}, status={}, opprettetTid={}, dryRun={}", LOGG_PREFIKS,
            forespørselSomBeholdes.getId(), forespørselSomBeholdes.getStatus(),
            forespørselSomBeholdes.getOpprettetTidspunkt(), dryRun);
        var forespørslerSomSkalUtgå = åpneForespørsler.stream()
            .filter(f -> !f.getId().equals(forespørselSomBeholdes.getId()))
            .toList();
        forespørslerSomSkalUtgå.forEach(f -> {
            var opprinneligStatus = f.getStatus();
            if (!dryRun) {
                forespørselBehandlingTjeneste.settForespørselTilUtgåttForvaltning(f.getUuid());
            }
            LOG.info("{}: UTGÅR id={}, type={}, opprinneligStatus={}, dryRun={}", LOGG_PREFIKS,
                f.getId(), f.getForespørselType(), opprinneligStatus, dryRun);
        });
        LOG.info("{}: Vurdert {} åpne forespørsler, {} skal utgå. dryRun={}", LOGG_PREFIKS,
            åpneForespørsler.size(), forespørslerSomSkalUtgå.size(), dryRun);
    }

    private static ForespørselEntitet finnForespørselSomBeholdes(List<ForespørselEntitet> åpneForespørsler) {
        var fagsystemForespørsler = åpneForespørsler.stream()
            .filter(f -> f.getForespørselType() == ForespørselType.BESTILT_AV_FAGSYSTEM)
            .toList();
        var kandidater = fagsystemForespørsler.isEmpty() ? åpneForespørsler : fagsystemForespørsler;
        return finnNyeste(kandidater);
    }

    private static ForespørselEntitet finnNyeste(List<ForespørselEntitet> forespørsler) {
        return forespørsler.stream()
            .max(Comparator.comparing(ForespørselEntitet::getOpprettetTidspunkt).thenComparing(ForespørselEntitet::getId))
            .orElseThrow();
    }

    private List<ForespørselEntitet> hentForespørsler(String saksnummer, String orgnummer, boolean dryRun) {
        var query = entityManager.createQuery(
                "from ForespørselEntitet where fagsystemSaksnummer = :saksnummer"
                    + " and organisasjonsnummer = :orgnummer"
                    + " and status in :statuser order by id",
                ForespørselEntitet.class)
            .setParameter(SAKSNUMMER, saksnummer)
            .setParameter(ORGNUMMER, orgnummer)
            .setParameter("statuser", ÅPNE_STATUSER);
        if (!dryRun) {
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }
        return query.getResultList();
    }

}
