package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Oppretter sak og oppgave hos arbeidsgiverportalen (min side arbeidsgiver) for en allerede lagret forespørsel.
 * Kjøres som egen task (i stedet for synkront i samme transaksjon som lagring av forespørsel) for at forespørselen
 * garantert er lagret/committet før vi gjør det eksterne kallet, og slik at en feilende/tidsavbrutt ekstern kall
 * automatisk prøves på nytt av prosesstask-rammeverket i stedet for å rulle tilbake forespørselen mens saken
 * likevel kan ha blitt opprettet hos arbeidsgiverportalen (som gir en foreldreløs sak uten tilhørende forespørsel).
 */
@ApplicationScoped
@ProsessTask(value = "forespørsel.opprettSakOgOppgave", maxFailedRuns = 5)
public class OpprettSakOgOppgaveTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OpprettSakOgOppgaveTask.class);

    private ForespørselTjeneste forespørselTjeneste;
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;

    OpprettSakOgOppgaveTask() {
        // CDI
    }

    @Inject
    public OpprettSakOgOppgaveTask(ForespørselTjeneste forespørselTjeneste, MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(ForespørselTaskProperties.KEY_FORESPOERSEL_UUID));
        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel " + forespørselUuid + " ved opprettelse av sak/oppgave"));

        if (forespørsel.arbeidsgiverNotifikasjonSakId() != null) {
            // Idempotens: unngår å opprette en ny sak dersom et tidligere (delvis) forsøk allerede har lykkes
            LOG.info("Sak er allerede opprettet for forespørsel {}, hopper over", forespørselUuid);
            return;
        }

        LOG.info("Oppretter sak og oppgave hos arbeidsgiverportalen for forespørsel {}", forespørselUuid);
        var sakId = minSideArbeidsgiverTjeneste.opprettSak(forespørsel);
        var oppgaveId = minSideArbeidsgiverTjeneste.opprettOppgave(forespørsel);
        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(forespørselUuid, sakId);
        forespørselTjeneste.setOppgaveId(forespørselUuid, oppgaveId);
        LOG.info("Opprettet sak {} og oppgave {} hos arbeidsgiverportalen for forespørsel {}", sakId, oppgaveId, forespørselUuid);
    }
}
