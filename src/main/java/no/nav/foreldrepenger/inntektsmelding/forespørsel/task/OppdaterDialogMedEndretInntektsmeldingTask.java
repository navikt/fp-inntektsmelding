package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;


@ApplicationScoped
@ProsessTask(value = "forespørsel.dialog.oppdater")
public class OppdaterDialogMedEndretInntektsmeldingTask extends ForespørselProsessTask {
    private static final Logger LOG = LoggerFactory.getLogger(OppdaterDialogMedEndretInntektsmeldingTask.class);

    private DialogportenTjeneste dialogportenTjeneste;

    OppdaterDialogMedEndretInntektsmeldingTask() {
        // CDI
    }

    @Inject
    public OppdaterDialogMedEndretInntektsmeldingTask(ForespørselTjeneste forespørselTjeneste, DialogportenTjeneste dialogportenTjeneste) {
        super(forespørselTjeneste);
        this.dialogportenTjeneste = dialogportenTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørsel = hentForespørsel(prosessTaskData);
        var inntektsmeldingUuid = hentInntektsmeldingUuid(prosessTaskData);

        LOG.info("Oppdaterer dialog hos Dialogporten med endret inntektsmelding for forespørsel {}", forespørsel.uuid());
        dialogportenTjeneste.utførMotDialogportenMedDevToleranse(() -> dialogportenTjeneste.oppdaterDialogMedEndretInntektsmelding(forespørsel, inntektsmeldingUuid));
        LOG.info("Oppdaterte dialog hos Dialogporten med endret inntektsmelding for forespørsel {}", forespørsel.uuid());
    }
}
