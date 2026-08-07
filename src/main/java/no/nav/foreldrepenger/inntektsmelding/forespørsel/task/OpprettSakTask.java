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
 * Oppretter sak hos arbeidsgiverportalen (min side arbeidsgiver) for en allerede lagret forespørsel.
 * Forutsetter at forespørselen er lagret/committet. Kjøres sekvensielt før {@link OpprettOppgaveTask}.
 */
@ApplicationScoped
@ProsessTask(value = "forespørsel.opprettSak")
public class OpprettSakTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OpprettSakTask.class);

    private ForespørselTjeneste forespørselTjeneste;
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;

    OpprettSakTask() {
        // CDI
    }

    @Inject
    public OpprettSakTask(ForespørselTjeneste forespørselTjeneste, MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(KEY_FORESPOERSEL_UUID));
        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel " + forespørselUuid + " ved opprettelse av sak"));

        if (forespørsel.arbeidsgiverNotifikasjonSakId() != null) {
            LOG.info("Sak er allerede opprettet for forespørsel {}, hopper over", forespørselUuid);
            return;
        }

        LOG.info("Oppretter sak hos arbeidsgiverportalen for forespørsel {}", forespørselUuid);
        var sakId = minSideArbeidsgiverTjeneste.opprettSak(forespørsel);
        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(forespørselUuid, sakId);
        LOG.info("Opprettet sak {} hos arbeidsgiverportalen for forespørsel {}", sakId, forespørselUuid);
    }
}
