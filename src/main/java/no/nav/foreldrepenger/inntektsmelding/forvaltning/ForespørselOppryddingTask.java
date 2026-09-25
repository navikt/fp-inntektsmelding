package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/**
 * Midlertidig engangsjobb (startes manuelt via {@link ProsessTaskRestTjeneste}) som rydder opp forespørsler
 * med status {@link ForespørselStatus#UNDER_BEHANDLING} som ikke lenger trengs, eller som er duplikater av en
 * annen åpen forespørsel på samme sak/arbeidsgiver. ALLE slike forespørsler behandles, uavhengig av alder.
 * <p>
 * Forespørslene slås sammen per unike (fagsystemSaksnummer, orgnummer)-kombinasjon, og fp-sak spørres én gang
 * per kombinasjon om behovet for inntektsmelding:
 * <ul>
 *     <li>TRENGS_IKKE: alle åpne forespørsler for kombinasjonen lukkes (settes til utgått).</li>
 *     <li>TRENGS: den nyeste forespørselen beholdes, eventuelle eldre duplikater lukkes.</li>
 *     <li>UKJENT: ingen forespørsler lukkes.</li>
 * </ul>
 * Jobben paginerer åpne forespørsler etter database-id, {@value #MAKS_RADER_PER_SIDE} rader om gangen (medlemslisten
 * for hver kombinasjon hentes likevel på nytt, uavhengig av sidevinduet, slik at duplikater på tvers av sider
 * håndteres korrekt). Fp-sak tillater maks {@value FpsakKlient#MAKS_ANTALL_FORESPØRSLER_PER_KALL} orgnumre per
 * saksnummer i ett kall (og ABAC-sjekken støtter uansett kun ett saksnummer per HTTP-kall), så kombinasjonene
 * grupperes per saksnummer før hvert kall. Skulle ett saksnummer i en side ha flere orgnumre enn dette, stoppes
 * siden tidlig og resten tas i neste task.
 * <p>
 * Selve lukkingen tar en pessimistisk database-lås på forespørselens id før den sjekker status på nytt. Slik
 * unngår vi at forespørselen har blitt endret av noe annet (f.eks. vanlig saksflyt) i tiden mellom vi leste
 * den og vi lukker den.
 */
@ApplicationScoped
@ProsessTask(value = "forespørsel.opprydding", maxFailedRuns = 1)
public class ForespørselOppryddingTask implements ProsessTaskHandler {

    static final String DRY_RUN = "dryRun";
    static final String FRA_ID = "fraId";

    /** Antall rader (ikke unike saksnummer/orgnummer-kombinasjoner) som hentes fra databasen per kjøring, for å avgjøre om det finnes flere sider. */
    private static final int MAKS_RADER_PER_SIDE = 500;

    private static final String LOGG_PREFIKS = "RYDD_FORESPØRSEL";

    private static final Logger LOG = LoggerFactory.getLogger(ForespørselOppryddingTask.class);

    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private ForespørselTjeneste forespørselTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private FpsakKlient fpsakKlient;

    ForespørselOppryddingTask() {
        // for CDI proxy
    }

    @Inject
    public ForespørselOppryddingTask(EntityManager entityManager,
                                      ProsessTaskTjeneste prosessTaskTjeneste,
                                      ForespørselTjeneste forespørselTjeneste,
                                      ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                      FpsakKlient fpsakKlient) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.forespørselTjeneste = forespørselTjeneste;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.fpsakKlient = fpsakKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).map(Boolean::valueOf).orElse(Boolean.TRUE);
        var fraId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_ID)).map(Long::valueOf).orElse(0L);

        var åpneForespørsler = hentÅpneForespørslerFraId(fraId);
        if (åpneForespørsler.isEmpty()) {
            LOG.info("{}: Ingen flere forespørsler med status UNDER_BEHANDLING funnet med id > {}. Jobben er ferdig.", LOGG_PREFIKS, fraId);
            return;
        }

        var orgnummerListePerSaksnummer = new LinkedHashMap<String, List<String>>();
        Long sisteBehandledeId = null;
        var nåddMaksAntall = false;
        for (var forespørsel : åpneForespørsler) {
            var saksnummer = forespørsel.getFagsystemSaksnummer().orElseThrow();
            var orgnummer = forespørsel.getOrganisasjonsnummer();
            var orgnummerListe = orgnummerListePerSaksnummer.computeIfAbsent(saksnummer, s -> new ArrayList<>());
            if (!orgnummerListe.contains(orgnummer)) {
                if (orgnummerListe.size() >= FpsakKlient.MAKS_ANTALL_FORESPØRSLER_PER_KALL) {
                    // Fp-sak tillater maks så mange orgnumre for ett saksnummer i ett kall. Stopper før denne raden
                    // slik at den (og resten av siden) blir vurdert på nytt i neste task.
                    nåddMaksAntall = true;
                    break;
                }
                orgnummerListe.add(orgnummer);
            }
            sisteBehandledeId = forespørsel.getId();
        }

        orgnummerListePerSaksnummer.forEach((saksnummer, orgnummerListe) -> behandleForSaksnummer(saksnummer, orgnummerListe, dryRun));

        if (nåddMaksAntall || åpneForespørsler.size() == MAKS_RADER_PER_SIDE) {
            // Enten stoppet vi tidlig pga. maks-taket, eller så fikk vi en full side og må anta at det finnes mer.
            lagNesteTask(sisteBehandledeId, dryRun);
        } else {
            LOG.info("{}: Ferdig med alle åpne forespørsler.", LOGG_PREFIKS);
        }
    }

    private void behandleForSaksnummer(String saksnummer, List<String> orgnummerListe, boolean dryRun) {
        var forespørslerTilFpsak = orgnummerListe.stream()
            .map(orgnummer -> new FpsakKlient.ForespørselStatusRequest.Forespørsel(saksnummer, orgnummer))
            .toList();

        var statusResponser = fpsakKlient.sjekkForespørselStatus(forespørslerTilFpsak);

        var åpneForespørslerPerOrgnummer = forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer(saksnummer)).stream()
            .collect(Collectors.groupingBy(f -> f.arbeidsgiver().orgnr()));

        statusResponser.forEach(statusRespons -> behandleSaksnummerOgOrgnummer(saksnummer, statusRespons,
            åpneForespørslerPerOrgnummer.getOrDefault(statusRespons.orgnummer(), List.of()), dryRun));
    }

    private void behandleSaksnummerOgOrgnummer(String saksnummer, FpsakKlient.ForespørselStatusResponse statusRespons,
                                                List<ForespørselDto> åpneForespørsler, boolean dryRun) {
        var orgnummer = statusRespons.orgnummer();
        if (åpneForespørsler.isEmpty()) {
            LOG.info("{}: saksnummer={} orgnummer={} har ingen åpne forespørsler lenger. Hopper over.", LOGG_PREFIKS, saksnummer, orgnummer);
            return;
        }

        åpneForespørsler.forEach(f -> LOG.info(
            "{}: fp-sak sin vurdering for id={} saksnummer={} orgnummer={} opprettet_tid={} er vurdering={} årsak={}", LOGG_PREFIKS,
            f.loepenr(), saksnummer, orgnummer, f.opprettetTidspunkt(), statusRespons.vurdering(), statusRespons.årsak()));

        var kandidaterTilLukking = switch (statusRespons.vurdering()) {
            case TRENGS_IKKE -> åpneForespørsler;
            case TRENGS -> duplikaterSomSkalLukkes(åpneForespørsler);
            case UKJENT -> List.<ForespørselDto>of();
        };

        if (dryRun || kandidaterTilLukking.isEmpty()) {
            return;
        }
        kandidaterTilLukking.forEach(this::lukkHvisFortsattUnderBehandling);
    }

    private List<ForespørselDto> duplikaterSomSkalLukkes(List<ForespørselDto> åpneForespørsler) {
        if (åpneForespørsler.size() <= 1) {
            return List.of();
        }
        var behold = åpneForespørsler.stream()
            .max(Comparator.comparing(ForespørselDto::opprettetTidspunkt).thenComparing(ForespørselDto::loepenr))
            .orElseThrow();
        return åpneForespørsler.stream().filter(f -> !f.uuid().equals(behold.uuid())).toList();
    }

    private void lukkHvisFortsattUnderBehandling(ForespørselDto forespørsel) {
        var låstEntitet = entityManager.find(ForespørselEntitet.class, forespørsel.loepenr(), LockModeType.PESSIMISTIC_WRITE);
        if (låstEntitet.getStatus() == ForespørselStatus.UNDER_BEHANDLING) {
            forespørselBehandlingTjeneste.settForespørselTilUtgåttForvaltning(forespørsel.uuid());
            LOG.info("{}: Lukket forespørsel id={} uuid={} (satt til utgått, forvaltning)", LOGG_PREFIKS, forespørsel.loepenr(), forespørsel.uuid());
        } else {
            LOG.info("{}: Forespørsel id={} uuid={} er ikke lenger UNDER_BEHANDLING. Hopper over lukking (idempotens).",
                LOGG_PREFIKS, forespørsel.loepenr(), forespørsel.uuid());
        }
    }

    private List<ForespørselEntitet> hentÅpneForespørslerFraId(long fraId) {
        var query = entityManager.createQuery("from ForespørselEntitet where id > :fraId and status = :status order by id",
            ForespørselEntitet.class);
        query.setParameter(FRA_ID, fraId);
        query.setParameter("status", ForespørselStatus.UNDER_BEHANDLING);
        query.setMaxResults(MAKS_RADER_PER_SIDE);
        return query.getResultList();
    }

    private void lagNesteTask(long nyFraId, boolean dryRun) {
        var nesteTask = ProsessTaskData.forProsessTask(ForespørselOppryddingTask.class);
        nesteTask.setProperty(FRA_ID, String.valueOf(nyFraId));
        nesteTask.setProperty(DRY_RUN, String.valueOf(dryRun));
        prosessTaskTjeneste.lagre(nesteTask);
    }
}
