package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@ProsessTask(value = "forespørsel.dialog.utgått")
public class SettDialogTilUtgåttTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SettDialogTilUtgåttTask.class);

    private ForespørselTaskTjeneste forespørselTaskTjeneste;
    private DialogportenTjeneste dialogportenTjeneste;

    SettDialogTilUtgåttTask() {
        // CDI
    }

    @Inject
    public SettDialogTilUtgåttTask(ForespørselTaskTjeneste forespørselTaskTjeneste, DialogportenTjeneste dialogportenTjeneste) {
        this.forespørselTaskTjeneste = forespørselTaskTjeneste;
        this.dialogportenTjeneste = dialogportenTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørsel = forespørselTaskTjeneste.hentForespørsel(prosessTaskData);

        LOG.info("Setter dialog hos Dialogporten til utgått for forespørsel {}", forespørsel.uuid());
        dialogportenTjeneste.utførMotDialogportenMedDevToleranse(() -> dialogportenTjeneste.settDialogTilUtgått(forespørsel));
        LOG.info("Satte dialog hos Dialogporten til utgått for forespørsel {}", forespørsel.uuid());
    }
}
