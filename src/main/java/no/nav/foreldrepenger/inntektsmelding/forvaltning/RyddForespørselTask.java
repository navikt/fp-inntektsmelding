package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakKlient;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.ForespørselVurderingResultat;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Midlertidig engangsjobb (sub-task til {@link RyddForespørslerTask}) som rydder opp forespørsler med
 * status {@link ForespørselStatus#UNDER_BEHANDLING} for én enkelt (saksnummer, orgnummer)-kombinasjon, basert på
 * svaret fra {@link FpsakKlient#sjekkForespørselStatus}:
 * <ul>
 *     <li>{@link ForespørselVurderingResultat#TRENGER_FORTSATT_INNTEKTSMELDING}: nyeste forespørsel beholdes,
 *     eventuelle eldre duplikater lukkes (settes til utgått).</li>
 *     <li>{@link ForespørselVurderingResultat#SETT_TIL_UTGÅTT}: alle åpne forespørsler for kombinasjonen settes til utgått.</li>
 *     <li>{@link ForespørselVurderingResultat#SETT_TIL_FERDIG}: alle åpne forespørsler for kombinasjonen settes
 *     til ferdig, slik at arbeidsgiver fortsatt kan sende inn en (ny) inntektsmelding så lenge
 *     saken løper.</li>
 * </ul>
 * Lukking tar en pessimistisk lås og sjekker status på nytt rett før skriving, for å unngå
 * kappløp med vanlig saksflyt i tiden mellom oppslag og skriving.
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
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private FpsakKlient fpsakKlient;

    RyddForespørselTask() {
        // for CDI proxy
    }

    @Inject
    public RyddForespørselTask(EntityManager entityManager,
                               ForespørselTjeneste forespørselTjeneste,
                               ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                               InntektsmeldingTjeneste inntektsmeldingTjeneste,
                               FpsakKlient fpsakKlient) {
        this.entityManager = entityManager;
        this.forespørselTjeneste = forespørselTjeneste;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
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

        var vurdering = fpsakKlient.sjekkForespørselStatus(saksnummer, orgnummer);

        switch (vurdering) {
            case TRENGER_FORTSATT_INNTEKTSMELDING -> lukkDuplikater(åpneForespørsler, dryRun, saksnummer, orgnummer);
            case SETT_TIL_UTGÅTT -> åpneForespørsler.forEach(
                f -> settTilUtgått(f, dryRun, saksnummer, orgnummer));
            case SETT_TIL_FERDIG -> åpneForespørsler.forEach(
                f -> ferdigstill(f, dryRun, saksnummer, orgnummer));
        }
    }

    private static String maskerOrgnummer(String orgnummer) {
        if (orgnummer == null) {
            return "";
        }
        var length = orgnummer.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        return "*".repeat(length - 4) + orgnummer.substring(length - 4);
    }

    private void lukkDuplikater(List<ForespørselDto> åpneForespørsler, boolean dryRun, String saksnummer,
                                String orgnummer) {
        var duplikaterForLukking = finnEldreDuplikaterSomSkalLukkes(åpneForespørsler, saksnummer, orgnummer);
        if (duplikaterForLukking.isEmpty()) {
            LOG.info("{}: saksnummer={}, orgnummer={}: Trenger fortsatt inntektsmelding, ingen duplikater å lukke.",
                LOGG_PREFIKS, saksnummer, maskerOrgnummer(orgnummer));
        }
        duplikaterForLukking.forEach(f -> settTilUtgått(f, dryRun, saksnummer, orgnummer));
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
            LOG.info("{}: saksnummer={}, orgnummer={}: Beholder uuid={}, lukker eldre duplikat(er) med uuid-ene {}",
                LOGG_PREFIKS, saksnummer, maskerOrgnummer(orgnummer), behold.uuid(),
                duplikaterForLukking.stream().map(ForespørselDto::uuid).toList());
        }

        return duplikaterForLukking;
    }

    private void settTilUtgått(ForespørselDto forespørsel, boolean dryRun, String saksnummer, String orgnummer) {
        if (dryRun) {
            LOG.info("{}: saksnummer={}, orgnummer={}: Skulle lukket forespørsel uuid={} "
                    + "(dry-run, ingen endring gjort)",
                LOGG_PREFIKS, saksnummer, maskerOrgnummer(orgnummer), forespørsel.uuid());
            return;
        }
        var forespørselEntitet = hentOgLåsForespørsel(forespørsel.uuid());
        if (forespørselEntitet.getStatus() == ForespørselStatus.UNDER_BEHANDLING) {
            forespørselBehandlingTjeneste.settForespørselTilUtgåttForvaltning(forespørsel.uuid());
            LOG.info("{}: saksnummer={}, orgnummer={}: Lukket forespørsel uuid={} (satt til utgått, forvaltning)",
                LOGG_PREFIKS, saksnummer, maskerOrgnummer(orgnummer), forespørsel.uuid());
        } else {
            // Status er endret av vanlig saksflyt siden oppslaget over - skulle ellers lukket forespørselen her.
            LOG.info("{}: saksnummer={}, orgnummer={}: Forespørsel uuid={} er ikke lenger UNDER_BEHANDLING. "
                    + "Hopper over lukking (idempotens).",
                LOGG_PREFIKS, saksnummer, maskerOrgnummer(orgnummer), forespørsel.uuid());
        }
    }

    private void ferdigstill(ForespørselDto forespørsel, boolean dryRun, String saksnummer, String orgnummer) {
        if (dryRun) {
            LOG.info("{}: saksnummer={}, orgnummer={}: Skulle satt forespørsel uuid={} til ferdig "
                    + "(dry-run, ingen endring gjort)",
                LOGG_PREFIKS, saksnummer, maskerOrgnummer(orgnummer), forespørsel.uuid());
            return;
        }
        var forespørselEntitet = hentOgLåsForespørsel(forespørsel.uuid());
        if (forespørselEntitet.getStatus() == ForespørselStatus.UNDER_BEHANDLING) {
            var sisteInntektsmelding = inntektsmeldingTjeneste.hentSisteInntektsmeldingForForespørsel(
                forespørsel.uuid());
            var inntektsmeldingUuid = Optional.ofNullable(sisteInntektsmelding)
                .map(InntektsmeldingDto::getInntektsmeldingUuid);
            forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørsel.uuid(), LukkeÅrsak.ORDINÆR_INNSENDING,
                inntektsmeldingUuid);
            LOG.info("{}: saksnummer={}, orgnummer={}: Satt forespørsel uuid={} til ferdig (forvaltning)",
                LOGG_PREFIKS, saksnummer, maskerOrgnummer(orgnummer), forespørsel.uuid());
        } else {
            // Status er endret av vanlig saksflyt siden oppslaget over - skulle ellers ferdigstilt forespørselen her.
            LOG.info("{}: saksnummer={}, orgnummer={}: Forespørsel uuid={} er ikke lenger UNDER_BEHANDLING. "
                    + "Hopper over ferdigstilling (idempotens).",
                LOGG_PREFIKS, saksnummer, maskerOrgnummer(orgnummer), forespørsel.uuid());
        }
    }

    private ForespørselEntitet hentOgLåsForespørsel(UUID uuid) {
        return entityManager.createQuery(
            "from ForespørselEntitet where uuid = :uuid", ForespørselEntitet.class)
            .setParameter("uuid", uuid)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getSingleResult();
    }
}
