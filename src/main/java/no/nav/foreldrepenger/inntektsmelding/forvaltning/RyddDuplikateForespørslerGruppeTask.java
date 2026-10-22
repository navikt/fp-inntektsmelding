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
@ProsessTask(value = "rydd.duplikate.forespørsler.gruppe", maxFailedRuns = 1)
public class RyddDuplikateForespørslerGruppeTask implements ProsessTaskHandler {

    static final String SAKSNUMMER = "saksnummer";
    static final String ORGNUMMER = "orgnummer";
    static final Set<ForespørselStatus> AKTIVE_STATUSER = Set.of(ForespørselStatus.UNDER_BEHANDLING, ForespørselStatus.FERDIG);

    private static final Logger LOG = LoggerFactory.getLogger(RyddDuplikateForespørslerGruppeTask.class);

    private EntityManager entityManager;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    RyddDuplikateForespørslerGruppeTask() {
        // CDI
    }

    @Inject
    public RyddDuplikateForespørslerGruppeTask(EntityManager entityManager,
                                            ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.entityManager = entityManager;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData data) {
        var saksnummer = data.getPropertyValue(SAKSNUMMER);
        var orgnummer = data.getPropertyValue(ORGNUMMER);
        var dryRun = !"false".equalsIgnoreCase(data.getPropertyValue(RyddDuplikateForespørslerTask.DRY_RUN));
        var aktive = hentGruppe(saksnummer, orgnummer, dryRun).stream()
            .filter(f -> AKTIVE_STATUSER.contains(f.getStatus()))
            .toList();

        if (aktive.size() < 2) {
            LOG.info("RYDD_DUPLIKATE_FORESPØRSLER: Ingen aktive duplikater. dryRun={}", dryRun);
            return;
        }
        var forespørselSomBeholdes = finnForespørselSomBeholdes(aktive);
        LOG.info("RYDD_DUPLIKATE_FORESPØRSLER: BEHOLD id={}, status={}, opprettetTid={}, dryRun={}",
            forespørselSomBeholdes.getId(), forespørselSomBeholdes.getStatus(),
            forespørselSomBeholdes.getOpprettetTidspunkt(), dryRun);
        var skalUtgå = aktive.stream().filter(f -> !f.getId().equals(forespørselSomBeholdes.getId())).toList();
        skalUtgå.forEach(f -> {
            var opprinneligStatus = f.getStatus();
            if (!dryRun) {
                forespørselBehandlingTjeneste.settForespørselTilUtgåttForvaltning(f.getUuid());
            }
            LOG.info("RYDD_DUPLIKATE_FORESPØRSLER: UTGÅR id={}, type={}, opprinneligStatus={}, dryRun={}",
                f.getId(), f.getForespørselType(), opprinneligStatus, dryRun);
        });
        LOG.info("RYDD_DUPLIKATE_FORESPØRSLER: Vurdert {} aktive forespørsler, {} skal utgå. dryRun={}",
            aktive.size(), skalUtgå.size(), dryRun);
    }

    private static ForespørselEntitet finnForespørselSomBeholdes(List<ForespørselEntitet> aktive) {
        var fagsystemForespørsler = aktive.stream()
            .filter(f -> f.getForespørselType() == ForespørselType.BESTILT_AV_FAGSYSTEM)
            .toList();
        var kandidater = fagsystemForespørsler.isEmpty() ? aktive : fagsystemForespørsler;
        return finnNyeste(kandidater);
    }

    private static ForespørselEntitet finnNyeste(List<ForespørselEntitet> forespørsler) {
        return forespørsler.stream()
            .max(Comparator.comparing(ForespørselEntitet::getOpprettetTidspunkt).thenComparing(ForespørselEntitet::getId))
            .orElseThrow();
    }

    private List<ForespørselEntitet> hentGruppe(String saksnummer, String orgnummer, boolean dryRun) {
        var query = entityManager.createQuery(
                "from ForespørselEntitet where fagsystemSaksnummer = :saksnummer"
                    + " and organisasjonsnummer = :orgnummer order by id",
                ForespørselEntitet.class)
            .setParameter(SAKSNUMMER, saksnummer)
            .setParameter(ORGNUMMER, orgnummer);
        if (!dryRun) {
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }
        return query.getResultList();
    }
}
