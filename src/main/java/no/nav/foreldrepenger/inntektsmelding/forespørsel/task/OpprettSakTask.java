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
 * Oppretter sak hos arbeidsgiverportalen (min side arbeidsgiver) for en allerede lagret forespørsel.
 * Forutsetter at forespørselen er lagret/committet. Kjøres sekvensielt før {@link OpprettOppgaveTask}.
 */
@ApplicationScoped
@ProsessTask(value = "forespørsel.opprett.sak")
public class OpprettSakTask extends ForespørselProsessTask {
    private static final Logger LOG = LoggerFactory.getLogger(OpprettSakTask.class);

    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;

    OpprettSakTask() {
        // CDI
    }

    @Inject
    public OpprettSakTask(ForespørselTjeneste forespørselTjeneste, MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste) {
        super(forespørselTjeneste);
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørsel = hentForespørsel(prosessTaskData);

        if (forespørsel.arbeidsgiverNotifikasjonSakId() != null) {
            LOG.info("Sak er allerede opprettet for forespørsel {}, hopper over", forespørsel.uuid());
            return;
        }

        LOG.info("Oppretter sak hos arbeidsgiverportalen for forespørsel {}", forespørsel.uuid());
        var sakId = minSideArbeidsgiverTjeneste.opprettSak(forespørsel);
        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(forespørsel.uuid(), sakId);
        LOG.info("Opprettet sak {} hos arbeidsgiverportalen for forespørsel {}", sakId, forespørsel.uuid());
    }
}
