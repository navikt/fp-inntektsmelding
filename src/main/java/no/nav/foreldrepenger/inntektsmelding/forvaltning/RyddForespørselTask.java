package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakKlient;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Midlertidig engangsjobb (sub-task til {@link RyddForespørslerTask}) som rydder opp forespørsler med
 * status {@link ForespørselStatus#UNDER_BEHANDLING} for én enkelt (saksnummer, orgnummer)-kombinasjon:
 * <ul>
 *     <li>trengerInntektsmelding=false: alle åpne forespørsler for kombinasjonen lukkes.</li>
 *     <li>trengerInntektsmelding=true: nyeste forespørsel beholdes, eventuelle eldre duplikater lukkes.</li>
 * </ul>
 * Lukking tar en pessimistisk lås og sjekker status på nytt rett før skriving, for å unngå kappløp med vanlig
 * saksflyt i tiden mellom oppslag og lukking.
 */
@ApplicationScoped
@ProsessTask(value = "rydd.forespørsel", maxFailedRuns = 1)
public class RyddForespørselTask implements ProsessTaskHandler {

    static final String SAKSNUMMER = "saksnummer";
    static final String ORGNUMMER = "orgnummer";
    static final String DRY_RUN = "dryRun";

    private static final String LOGG_PREFIKS = "RYDD_FORESPØRSEL";

    private static final Logger LOG = LoggerFactory.getLogger(RyddForespørselTask.class);

    private EntityManager entityManager;
    private ForespørselTjeneste forespørselTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private FpsakKlient fpsakKlient;

    RyddForespørselTask() {
        // for CDI proxy
    }

    @Inject
    public RyddForespørselTask(EntityManager entityManager,
                               ForespørselTjeneste forespørselTjeneste,
                               ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                               FpsakKlient fpsakKlient) {
        this.entityManager = entityManager;
        this.forespørselTjeneste = forespørselTjeneste;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.fpsakKlient = fpsakKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = prosessTaskData.getPropertyValue(SAKSNUMMER);
        var orgnummer = prosessTaskData.getPropertyValue(ORGNUMMER);
        var dryRun = !"false".equalsIgnoreCase(prosessTaskData.getPropertyValue(DRY_RUN));

        var åpneForespørsler = forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer(saksnummer)).stream()
            .filter(f -> f.arbeidsgiver().orgnr().equals(orgnummer))
            .toList();

        if (åpneForespørsler.isEmpty()) {
            return;
        }

        var trengerInntektsmelding = fpsakKlient.sjekkForespørselStatus(saksnummer, orgnummer);

        var kandidaterTilLukking = trengerInntektsmelding
                                   ? finnEldreDuplikaterSomSkalLukkes(åpneForespørsler, saksnummer, orgnummer)
                                   : åpneForespørsler;

        if (trengerInntektsmelding && kandidaterTilLukking.isEmpty()) {
            LOG.info("{}: saksnummer={}, orgnummer={}: Trenger fortsatt inntektsmelding, ingen duplikater å lukke.",
                LOGG_PREFIKS, saksnummer, orgnummer);
        }

        kandidaterTilLukking.forEach(f -> lukkHvisFortsattUnderBehandling(f, dryRun, saksnummer, orgnummer));
    }

    private static List<ForespørselDto> finnEldreDuplikaterSomSkalLukkes(List<ForespørselDto> åpneForespørsler,
                                                                         String saksnummer,
                                                                         String orgnummer) {
        if (åpneForespørsler.size() <= 1) {
            return List.of();
        }
        var behold = åpneForespørsler.stream()
            .max(Comparator.comparing(ForespørselDto::opprettetTidspunkt).thenComparing(ForespørselDto::loepenr))
            .orElseThrow();
        var duplikaterForLukking = åpneForespørsler.stream().filter(f -> !f.uuid().equals(behold.uuid())).toList();

        if (!duplikaterForLukking.isEmpty()) {
            LOG.info("{}: saksnummer={}, orgnummer={}: Beholder id={}, lukker eldre duplikat(er) med id-ene {}",
                LOGG_PREFIKS, saksnummer, orgnummer, behold.loepenr(),
                duplikaterForLukking.stream().map(ForespørselDto::loepenr).toList());
        }

        return duplikaterForLukking;
    }

    private void lukkHvisFortsattUnderBehandling(ForespørselDto forespørsel, boolean dryRun, String saksnummer,
                                                 String orgnummer) {
        if (dryRun) {
            LOG.info("{}: saksnummer={}, orgnummer={}: Skulle lukket forespørsel id={} uuid={} "
                    + "(dry-run, ingen endring gjort)",
                LOGG_PREFIKS, saksnummer, orgnummer, forespørsel.loepenr(), forespørsel.uuid());
            return;
        }
        var forespørselEntitet = entityManager.find(ForespørselEntitet.class, forespørsel.loepenr(),
            LockModeType.PESSIMISTIC_WRITE);
        if (forespørselEntitet.getStatus() == ForespørselStatus.UNDER_BEHANDLING) {
            forespørselBehandlingTjeneste.settForespørselTilUtgåttForvaltning(forespørsel.uuid());
            LOG.info("{}: saksnummer={}, orgnummer={}: Lukket forespørsel id={} uuid={} (satt til utgått, forvaltning)",
                LOGG_PREFIKS, saksnummer, orgnummer, forespørsel.loepenr(), forespørsel.uuid());
        } else {
            LOG.info("{}: saksnummer={}, orgnummer={}: Forespørsel id={} uuid={} er ikke lenger UNDER_BEHANDLING. "
                    + "Hopper over lukking (idempotens).",
                LOGG_PREFIKS, saksnummer, orgnummer, forespørsel.loepenr(), forespørsel.uuid());
        }
    }
}
