package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.util.Optional;
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


@ApplicationScoped
@ProsessTask(value = "forespørsel.dialog.oppdater")
public class OppdaterDialogMedEndretInntektsmeldingTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OppdaterDialogMedEndretInntektsmeldingTask.class);

    private ForespørselTjeneste forespørselTjeneste;
    private DialogportenTjeneste dialogportenTjeneste;

    OppdaterDialogMedEndretInntektsmeldingTask() {
        // CDI
    }

    @Inject
    public OppdaterDialogMedEndretInntektsmeldingTask(ForespørselTjeneste forespørselTjeneste, DialogportenTjeneste dialogportenTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.dialogportenTjeneste = dialogportenTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID));
        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel " + forespørselUuid + " ved oppdatering av dialog"));
        var inntektsmeldingUuid = Optional.ofNullable(prosessTaskData.getPropertyValue(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID)).map(UUID::fromString);

        LOG.info("Oppdaterer dialog hos Dialogporten med endret inntektsmelding for forespørsel {}", forespørselUuid);
        dialogportenTjeneste.utførMotDialogportenMedDevToleranse(() -> dialogportenTjeneste.oppdaterDialogMedEndretInntektsmelding(forespørsel, inntektsmeldingUuid));
        LOG.info("Oppdaterte dialog hos Dialogporten med endret inntektsmelding for forespørsel {}", forespørselUuid);
    }
}
