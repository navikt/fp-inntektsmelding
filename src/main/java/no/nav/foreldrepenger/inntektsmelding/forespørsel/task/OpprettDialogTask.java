package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/**
 * Selve Dialogporten-kallet gjøres via {@link DialogportenTjeneste#utførMotDialogportenMedDevToleranse}, som
 * svelger feil i dev (der Dialogporten-oppsettet er kjent ufullstendig) for å unngå at tasken hoper seg opp som
 * feilet der, mens feil i prod fortsatt kastes videre slik at prosesstask-rammeverket prøver på nytt som normalt.
 */
@ApplicationScoped
@ProsessTask(value = "forespørsel.opprettDialog")
public class OpprettDialogTask extends ForespørselProsessTask {
    private static final Logger LOG = LoggerFactory.getLogger(OpprettDialogTask.class);

    private DialogportenTjeneste dialogportenTjeneste;

    OpprettDialogTask() {
        // CDI
    }

    @Inject
    public OpprettDialogTask(ForespørselTjeneste forespørselTjeneste, DialogportenTjeneste dialogportenTjeneste) {
        super(forespørselTjeneste);
        this.dialogportenTjeneste = dialogportenTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørsel = hentForespørsel(prosessTaskData);

        if (forespørsel.dialogportenUuid() != null) {
            // Idempotens: unngår å opprette en ny dialog dersom et tidligere (delvis) forsøk allerede har lykkes
            LOG.info("Dialog er allerede opprettet for forespørsel {}, hopper over", forespørsel.uuid());
            return;
        }

        LOG.info("Oppretter dialog hos Dialogporten for forespørsel {}", forespørsel.uuid());
        dialogportenTjeneste.utførMotDialogportenMedDevToleranse(() -> {
            var dialogportenUuid = dialogportenTjeneste.opprettDialog(forespørsel);
            forespørselTjeneste.setDialogportenUuid(forespørsel.uuid(), dialogportenUuid);
            LOG.info("Opprettet dialog {} hos Dialogporten for forespørsel {}", dialogportenUuid, forespørsel.uuid());
        });
    }
}
