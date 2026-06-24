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

@ApplicationScoped
@ProsessTask(value = "utgått.sak.dialogporten")
public class UtgåSakDialogportenTask implements ProsessTaskHandler {
    private static final String FORESPØRSEL_UUID = "forespørselUuid";
    private static final Logger LOG = LoggerFactory.getLogger(UtgåSakDialogportenTask.class);
    private DialogportenTjeneste dialogportenTjeneste;
    private ForespørselTjeneste forespørselTjeneste;

    UtgåSakDialogportenTask() {
        // CDI
    }

    @Inject
    UtgåSakDialogportenTask(DialogportenTjeneste dialogportenTjeneste,
                            ForespørselTjeneste forespørselTjeneste) {
        this.dialogportenTjeneste = dialogportenTjeneste;
        this.forespørselTjeneste = forespørselTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FORESPØRSEL_UUID));
        var forespørselDto = forespørselTjeneste.hentForespørsel(forespørselUuid).orElseThrow();
        LOG.info("Starter task for å sette sak til utgått i dialogporten for saksnummer {} og orgnr {}", forespørselDto.fagsystemSaksnummer(), forespørselDto.arbeidsgiver());
        dialogportenTjeneste.settSakTilUtgått(forespørselDto);
        LOG.info("Sluttfører task for å sette sak til utgått i dialogporten for saksnummer {} og orgnr {}", forespørselDto.fagsystemSaksnummer(), forespørselDto.arbeidsgiver());
    }

    public static ProsessTaskData lagTask(UUID forespørselUuid) {
        var task = ProsessTaskData.forProsessTask(UtgåSakDialogportenTask.class);
        // Viktig at tasker som skal oppdatere forespørselentitet alle deler samme gruppe (forespørselUuid) så vi ikke får lås
        task.setGruppe(forespørselUuid.toString());
        task.setProperty(FORESPØRSEL_UUID, forespørselUuid.toString());
        return task;
    }
}
