package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Oppretter dialog hos Dialogporten for en allerede lagret forespørsel.
 * Kjøres som egen task av samme grunn som {@link OpprettSakOgOppgaveTask}: forespørselen er garantert lagret/committet
 * før det eksterne kallet gjøres, og feilende kall prøves automatisk på nytt av prosesstask-rammeverket i stedet for
 * å kaste og eventuelt rulle tilbake forespørselen i den transaksjonen som opprettet den.
 */
@ApplicationScoped
@ProsessTask(value = "forespørsel.opprettDialog", maxFailedRuns = 5)
public class OpprettDialogTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OpprettDialogTask.class);

    private ForespørselTjeneste forespørselTjeneste;
    private DialogportenTjeneste dialogportenTjeneste;

    OpprettDialogTask() {
        // CDI
    }

    @Inject
    public OpprettDialogTask(ForespørselTjeneste forespørselTjeneste, DialogportenTjeneste dialogportenTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.dialogportenTjeneste = dialogportenTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(ForespørselTaskProperties.KEY_FORESPOERSEL_UUID));
        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel " + forespørselUuid + " ved opprettelse av dialog"));

        if (forespørsel.dialogportenUuid() != null) {
            // Idempotens: unngår å opprette en ny dialog dersom et tidligere (delvis) forsøk allerede har lykkes
            LOG.info("Dialog er allerede opprettet for forespørsel {}, hopper over", forespørselUuid);
            return;
        }

        LOG.info("Oppretter dialog hos Dialogporten for forespørsel {}", forespørselUuid);
        var dialogportenUuid = dialogportenTjeneste.opprettDialog(forespørsel);
        forespørselTjeneste.setDialogportenUuid(forespørselUuid, dialogportenUuid);
        LOG.info("Opprettet dialog {} hos Dialogporten for forespørsel {}", dialogportenUuid, forespørselUuid);
    }
}
