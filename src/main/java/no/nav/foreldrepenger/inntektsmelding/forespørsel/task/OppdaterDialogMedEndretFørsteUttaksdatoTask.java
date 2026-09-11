package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.util.Comparator;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselEndringHistorikkDto;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "forespørsel.dialog.endreUttaksdato")
public class OppdaterDialogMedEndretFørsteUttaksdatoTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OppdaterDialogMedEndretFørsteUttaksdatoTask.class);

    private ForespørselTjeneste forespørselTjeneste;
    private DialogportenTjeneste dialogportenTjeneste;

    OppdaterDialogMedEndretFørsteUttaksdatoTask() {
        // CDI
    }

    @Inject
    public OppdaterDialogMedEndretFørsteUttaksdatoTask(ForespørselTjeneste forespørselTjeneste,
                                                     DialogportenTjeneste dialogportenTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.dialogportenTjeneste = dialogportenTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID));
        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel " + forespørselUuid + " ved oppdatering av første uttaksdato"));
        if (forespørsel.dialogportenUuid() == null) {
            throw new IllegalStateException("Mangler dialogportenUuid for forespørsel " + forespørselUuid);
        }
        if (forespørsel.status().equals(ForespørselStatus.UTGÅTT)) {
            LOG.info("Forespørsel {} er utgått, oppdaterer ikke dialog", forespørselUuid);
            return;
        }
        var sisteEndring = forespørsel.historiskeEndringer().stream().max(Comparator.comparing(ForespørselEndringHistorikkDto::opprettetTid)).orElseThrow();
        dialogportenTjeneste.utførMotDialogportenMedDevToleranse(
            () -> dialogportenTjeneste.oppdaterDialogMedEndretFørsteUttaksdato(forespørsel, sisteEndring.førsteUttaksdato(), forespørsel.førsteUttaksdato()));
        LOG.info("Oppdaterte Dialogporten med endret første uttaksdato for forespørsel {}", forespørselUuid);
    }
}
