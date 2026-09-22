package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/**
 * Midlertidig engangsjobb (startes manuelt via {@link ProsessTaskRestTjeneste}) som rydder opp forespørsler som
 * ikke lenger trengs, eller som er duplikater av en annen åpen forespørsel på samme sak/arbeidsgiver.
 * <p>
 * Jobben behandler ALLE forespørsler med status {@link ForespørselStatus#UNDER_BEHANDLING}, uavhengig av alder
 * (den vanlige to-månedersgrensen for forvaltningsendepunkter gjelder ikke her).
 * <p>
 * Forespørslene grupperes på (fagsystemSaksnummer, orgnummer). For hver gruppe blir fp-sak spurt én gang om
 * behovet for inntektsmelding er TRENGS, TRENGS_IKKE eller UKJENT:
 * <ul>
 *     <li>TRENGS_IKKE: alle åpne forespørsler i gruppen lukkes (settes til utgått).</li>
 *     <li>TRENGS: den nyeste forespørselen i gruppen beholdes, eventuelle eldre duplikater lukkes.</li>
 *     <li>UKJENT: ingen forespørsler i gruppen lukkes.</li>
 * </ul>
 * Jobben deler seg selv opp i bolker (maks {@value FpsakKlient#MAKS_ANTALL_FORESPØRSLER_PER_KALL} grupper totalt per
 * task) ved å keysette-paginere åpne forespørsler etter database-id. Selve fp-sak-kallet skjer kun én gang per
 * gruppe per kjøring, men gruppens fullstendige medlemsliste hentes på nytt (uavhengig av sidevinduet) slik at
 * duplikater som havner på tvers av sidegrenser håndteres korrekt.
 * <p>
 * Fp-saks ABAC-sjekk støtter kun ett unikt saksnummer per HTTP-kall. Gruppene deles derfor opp per saksnummer og det
 * gjøres ett fp-sak-kall per saksnummer (med alle orgnummer-gruppene for det saksnummeret i samme kall). Svaret
 * valideres strengt 1:1 mot forespørselen: manglende, duplikate eller uventede svar feiler tasken eksplisitt
 * (kastes som {@link TekniskException}) i stedet for å bli logget bort som UKJENT.
 * <p>
 * Selve lukkingen skjer med en pessimistisk database-lås på forespørselens id, slik at les-sjekk-skriv av status
 * ({@link ForespørselStatus#UNDER_BEHANDLING}) blir atomisk innenfor prosesstaskens transaksjon og ikke kan
 * kappløpe med andre samtidige oppdateringer av samme rad.
 */
@ApplicationScoped
@ProsessTask(value = "forespørsel.rydd.mot.fpsak", maxFailedRuns = 1)
public class LukkForespørslerUtenBehovTask implements ProsessTaskHandler {

    static final String DRY_RUN = "dryRun";
    static final String FRA_ID = "fraId";

    /** Antall rader (ikke grupper) som hentes fra databasen per kjøring, for å avgjøre om det finnes flere sider. */
    private static final int MAKS_RADER_PER_SIDE = 500;

    private static final String LOGG_PREFIKS = "RYDD_FORESPØRSEL_FPSAK_STATUS";

    private static final Logger LOG = LoggerFactory.getLogger(LukkForespørslerUtenBehovTask.class);

    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private ForespørselTjeneste forespørselTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private FpsakKlient fpsakKlient;

    LukkForespørslerUtenBehovTask() {
        // for CDI proxy
    }

    @Inject
    public LukkForespørslerUtenBehovTask(EntityManager entityManager,
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

        var side = hentÅpneForespørslerFraId(fraId);
        if (side.isEmpty()) {
            LOG.info("{}: Ingen flere forespørsler med status UNDER_BEHANDLING funnet med id > {}. Jobben er ferdig.", LOGG_PREFIKS, fraId);
            return;
        }

        var grupper = new LinkedHashMap<GruppeId, Ytelsetype>();
        Long sisteBehandledeId = null;
        var nåddGruppetak = false;
        for (var forespørsel : side) {
            var saksnummerOpt = forespørsel.getFagsystemSaksnummer().filter(s -> !s.isBlank());
            if (saksnummerOpt.isEmpty()) {
                // Kan ikke vurderes mot fp-sak uten et fagsystem-saksnummer (f.eks. arbeidsgiverinitierte forespørsler).
                sisteBehandledeId = forespørsel.getId();
                continue;
            }
            var gruppeId = new GruppeId(saksnummerOpt.get(), forespørsel.getOrganisasjonsnummer());
            if (!grupper.containsKey(gruppeId)) {
                if (grupper.size() >= FpsakKlient.MAKS_ANTALL_FORESPØRSLER_PER_KALL) {
                    // Stopper før denne raden slik at den (og resten av siden) blir vurdert på nytt i neste task.
                    nåddGruppetak = true;
                    break;
                }
                grupper.put(gruppeId, forespørsel.getYtelseType());
            }
            sisteBehandledeId = forespørsel.getId();
        }

        if (!grupper.isEmpty()) {
            behandleGrupper(grupper, dryRun);
        }

        if (nåddGruppetak || side.size() == MAKS_RADER_PER_SIDE) {
            // Enten stoppet vi tidlig pga. gruppetaket, eller så fikk vi en full side og må anta at det finnes mer.
            lagNesteTask(sisteBehandledeId, dryRun);
        } else {
            LOG.info("{}: Ferdig med alle åpne forespørsler. Siste behandlede id var {}.", LOGG_PREFIKS, sisteBehandledeId);
        }
    }

    private void behandleGrupper(Map<GruppeId, Ytelsetype> grupper, boolean dryRun) {
        // Fp-saks ABAC-sjekk støtter kun ett unikt saksnummer per HTTP-kall, så vi må kalle fp-sak separat per
        // saksnummer (med alle orgnummer-gruppene for det saksnummeret samlet i ett kall).
        var grupperPerSaksnummer = new LinkedHashMap<String, List<GruppeId>>();
        grupper.keySet().forEach(g -> grupperPerSaksnummer.computeIfAbsent(g.saksnummer(), s -> new ArrayList<>()).add(g));

        for (var entry : grupperPerSaksnummer.entrySet()) {
            var saksnummer = entry.getKey();
            var gruppeIdListe = entry.getValue();
            var forespørslerTilFpsak = gruppeIdListe.stream()
                .map(g -> new FpsakKlient.ForespørselStatusRequest.ForespørselStatusForespørsel(g.saksnummer(), g.orgnummer(),
                    mapYtelse(grupper.get(g))))
                .toList();

            // Fp-sak-feil skal feile tasken (og dermed hele kjøringen for denne bolken), ikke skjules som UKJENT.
            var svar = fpsakKlient.sjekkForespørselStatus(forespørslerTilFpsak);
            var svarPerGruppe = validerSvar1Til1(saksnummer, gruppeIdListe, svar);

            for (var gruppeId : gruppeIdListe) {
                behandleGruppe(gruppeId, svarPerGruppe.get(gruppeId), dryRun);
            }
        }
    }

    /**
     * Validerer at fp-sak har svart nøyaktig én gang per forespurt gruppe for dette saksnummeret: ingen manglende,
     * ingen duplikate og ingen uventede/ekstra svar. Ethvert avvik feiler tasken eksplisitt (i stedet for å bli
     * logget bort og behandlet som UKJENT), slik at inkonsistente svar fra fp-sak ikke kan føre til at forespørsler
     * lukkes (eller ikke lukkes) på feil grunnlag.
     */
    private Map<GruppeId, FpsakKlient.ForespørselStatusResponse> validerSvar1Til1(String saksnummer, List<GruppeId> forventedeGrupper,
                                                                                  List<FpsakKlient.ForespørselStatusResponse> svar) {
        var svarPerGruppe = new LinkedHashMap<GruppeId, FpsakKlient.ForespørselStatusResponse>();
        for (var s : svar) {
            var gruppeId = new GruppeId(s.fagsakSaksnummer(), s.orgnummer());
            if (!forventedeGrupper.contains(gruppeId)) {
                throw new TekniskException("FPINNTEKTSMELDING-694580",
                    "Fikk uventet svar fra fp-sak for saksnummer=%s orgnummer=%s som ikke var forespurt (kall for saksnummer=%s)".formatted(
                        gruppeId.saksnummer(), gruppeId.orgnummer(), saksnummer));
            }
            if (svarPerGruppe.putIfAbsent(gruppeId, s) != null) {
                throw new TekniskException("FPINNTEKTSMELDING-694581",
                    "Fikk duplikat svar fra fp-sak for saksnummer=%s orgnummer=%s".formatted(gruppeId.saksnummer(), gruppeId.orgnummer()));
            }
        }
        var manglende = forventedeGrupper.stream().filter(g -> !svarPerGruppe.containsKey(g)).toList();
        if (!manglende.isEmpty()) {
            throw new TekniskException("FPINNTEKTSMELDING-694582",
                "Manglet svar fra fp-sak for %d av %d forespurte grupper for saksnummer=%s: %s".formatted(manglende.size(),
                    forventedeGrupper.size(), saksnummer, manglende));
        }
        return svarPerGruppe;
    }

    private void behandleGruppe(GruppeId gruppeId, FpsakKlient.ForespørselStatusResponse svar, boolean dryRun) {
        var åpneIGruppen = hentÅpneIGruppen(gruppeId);
        if (åpneIGruppen.isEmpty()) {
            LOG.info("{}: Gruppe saksnummer={} orgnummer={} har ingen åpne forespørsler lenger. Hopper over.", LOGG_PREFIKS,
                gruppeId.saksnummer(), gruppeId.orgnummer());
            return;
        }

        åpneIGruppen.forEach(f -> LOG.info("{}: id={} saksnummer={} orgnummer={} opprettet_tid={} vurdering={} årsak={}", LOGG_PREFIKS,
            f.loepenr(), gruppeId.saksnummer(), gruppeId.orgnummer(), f.opprettetTidspunkt(), svar.vurdering(), svar.årsak()));

        var kandidaterTilLukking = switch (svar.vurdering()) {
            case TRENGS_IKKE -> åpneIGruppen;
            case TRENGS -> duplikaterSomSkalLukkes(åpneIGruppen);
            case UKJENT -> List.<ForespørselDto>of();
        };

        if (dryRun || kandidaterTilLukking.isEmpty()) {
            return;
        }
        kandidaterTilLukking.forEach(this::lukkHvisFortsattUnderBehandling);
    }

    /** TRENGS: behold forespørselen som kom inn sist (størst opprettetTidspunkt, id som tie-breaker), lukk resten. */
    private List<ForespørselDto> duplikaterSomSkalLukkes(List<ForespørselDto> åpneIGruppen) {
        if (åpneIGruppen.size() <= 1) {
            return List.of();
        }
        var behold = åpneIGruppen.stream()
            .max(Comparator.comparing(ForespørselDto::opprettetTidspunkt).thenComparing(ForespørselDto::loepenr))
            .orElseThrow();
        return åpneIGruppen.stream().filter(f -> !f.uuid().equals(behold.uuid())).toList();
    }

    private void lukkHvisFortsattUnderBehandling(ForespørselDto forespørsel) {
        // Les-sjekk-skriv må være atomisk for å unngå kappløp med andre samtidige oppdateringer av samme rad
        // (f.eks. at forespørselen blir ferdigstilt eller lukket av vanlig saksflyt mellom lesing og lukking her).
        // Vi låser derfor raden pessimistisk på database-id og verifiserer uuid + status UNDER_BEHANDLING under
        // låsen, før ForespørselBehandlingTjeneste kalles. Låsen holdes til prosesstaskens transaksjon commiter.
        var låstEntitet = entityManager.find(ForespørselEntitet.class, forespørsel.loepenr(), LockModeType.PESSIMISTIC_WRITE);
        var kanLukkes = låstEntitet != null && forespørsel.uuid().equals(låstEntitet.getUuid())
            && låstEntitet.getStatus() == ForespørselStatus.UNDER_BEHANDLING;
        if (kanLukkes) {
            forespørselBehandlingTjeneste.settForespørselTilUtgåttForvaltning(forespørsel.uuid());
            LOG.info("{}: Lukket forespørsel id={} uuid={} (satt til utgått, forvaltning)", LOGG_PREFIKS, forespørsel.loepenr(), forespørsel.uuid());
        } else {
            LOG.info("{}: Forespørsel id={} uuid={} er ikke lenger UNDER_BEHANDLING (eller uuid stemmer ikke). Hopper over lukking (idempotens).",
                LOGG_PREFIKS, forespørsel.loepenr(), forespørsel.uuid());
        }
    }

    private List<ForespørselDto> hentÅpneIGruppen(GruppeId gruppeId) {
        return forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer(gruppeId.saksnummer())).stream()
            .filter(f -> gruppeId.orgnummer().equals(f.arbeidsgiver().orgnr()))
            .toList();
    }

    private List<ForespørselEntitet> hentÅpneForespørslerFraId(long fraId) {
        var query = entityManager.createQuery("from ForespørselEntitet where id > :fraId and status = :status order by id",
            ForespørselEntitet.class);
        query.setParameter("fraId", fraId);
        query.setParameter("status", ForespørselStatus.UNDER_BEHANDLING);
        query.setMaxResults(MAKS_RADER_PER_SIDE);
        return query.getResultList();
    }

    private void lagNesteTask(long nyFraId, boolean dryRun) {
        var nesteTask = ProsessTaskData.forProsessTask(LukkForespørslerUtenBehovTask.class);
        nesteTask.setProperty(FRA_ID, String.valueOf(nyFraId));
        nesteTask.setProperty(DRY_RUN, String.valueOf(dryRun));
        prosessTaskTjeneste.lagre(nesteTask);
    }

    private static FpsakKlient.ForespørselStatusRequest.Ytelse mapYtelse(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> FpsakKlient.ForespørselStatusRequest.Ytelse.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> FpsakKlient.ForespørselStatusRequest.Ytelse.SVANGERSKAPSPENGER;
        };
    }

    private record GruppeId(String saksnummer, String orgnummer) {
    }
}
