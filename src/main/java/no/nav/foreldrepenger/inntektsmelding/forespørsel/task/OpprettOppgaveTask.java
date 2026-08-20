package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/**
 * Oppretter oppgave hos arbeidsgiverportalen (min side arbeidsgiver) for en allerede lagret forespørsel.
 * Kjøres sekvensielt etter {@link OpprettSakTask}.
 */
@ApplicationScoped
@ProsessTask(value = "forespørsel.opprett.oppgave")
public class OpprettOppgaveTask extends ForespørselProsessTask {
    private static final Logger LOG = LoggerFactory.getLogger(OpprettOppgaveTask.class);

    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;

    OpprettOppgaveTask() {
        // CDI
    }

    @Inject
    public OpprettOppgaveTask(ForespørselTjeneste forespørselTjeneste, MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste) {
        super(forespørselTjeneste);
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørsel = hentForespørsel(prosessTaskData);

        if (forespørsel.oppgaveId() != null) {
            LOG.info("Oppgave er allerede opprettet for forespørsel {}, hopper over", forespørsel.uuid());
            return;
        }

        LOG.info("Oppretter oppgave hos arbeidsgiverportalen for forespørsel {}", forespørsel.uuid());
        var oppgaveId = minSideArbeidsgiverTjeneste.opprettOppgave(forespørsel);
        forespørselTjeneste.setOppgaveId(forespørsel.uuid(), oppgaveId);
        LOG.info("Opprettet oppgave {} hos arbeidsgiverportalen for forespørsel {}", oppgaveId, forespørsel.uuid());
    }
}
