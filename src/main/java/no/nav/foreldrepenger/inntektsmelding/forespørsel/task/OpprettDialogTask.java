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
 * Selve Dialogporten-kallet gjøres via {@link DialogportenTjeneste#utførMotDialogportenMedDevToleranse}, som
 * svelger feil i dev (der Dialogporten-oppsettet er kjent ufullstendig) for å unngå at tasken hoper seg opp som
 * feilet der, mens feil i prod fortsatt kastes videre slik at prosesstask-rammeverket prøver på nytt som normalt.
 */
@ApplicationScoped
@ProsessTask(value = "forespørsel.opprettDialog")
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
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID));
        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel " + forespørselUuid + " ved opprettelse av dialog"));

        if (forespørsel.dialogportenUuid() != null) {
            // Idempotens: unngår å opprette en ny dialog dersom et tidligere (delvis) forsøk allerede har lykkes
            LOG.info("Dialog er allerede opprettet for forespørsel {}, hopper over", forespørselUuid);
            return;
        }

        LOG.info("Oppretter dialog hos Dialogporten for forespørsel {}", forespørselUuid);
        dialogportenTjeneste.utførMotDialogportenMedDevToleranse(() -> {
            var dialogportenUuid = dialogportenTjeneste.opprettDialog(forespørsel);
            forespørselTjeneste.setDialogportenUuid(forespørselUuid, dialogportenUuid);
            LOG.info("Opprettet dialog {} hos Dialogporten for forespørsel {}", dialogportenUuid, forespørselUuid);
        });
    }
}
