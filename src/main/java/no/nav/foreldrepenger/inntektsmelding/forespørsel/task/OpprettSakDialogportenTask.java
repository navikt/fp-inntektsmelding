package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenTjeneste;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "opprett.sak.dialogporten")
public class OpprettSakDialogportenTask implements ProsessTaskHandler {
    private static final String FORESPØRSEL_UUID = "forespørselUuid";
    private static final Logger LOG = LoggerFactory.getLogger(OpprettSakDialogportenTask.class);
    private DialogportenTjeneste dialogportenTjeneste;
    private ForespørselTjeneste forespørselTjeneste;

    OpprettSakDialogportenTask() {
        // CDI
    }

    @Inject
    OpprettSakDialogportenTask(DialogportenTjeneste dialogportenTjeneste,
                               ForespørselTjeneste forespørselTjeneste) {
        this.dialogportenTjeneste = dialogportenTjeneste;
        this.forespørselTjeneste = forespørselTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FORESPØRSEL_UUID));
        var forespørselDto = forespørselTjeneste.hentForespørsel(forespørselUuid).orElseThrow();
        LOG.info("Starter task for opprettelse av sak i dialogporten for saksnummer {} og orgnr {}", forespørselDto.fagsystemSaksnummer(), forespørselDto.arbeidsgiver());
        try {
            var dialogportenUuid = dialogportenTjeneste.opprettSakDialogporten(forespørselDto);
            forespørselTjeneste.setDialogportenUuid(forespørselUuid, dialogportenUuid);
        } catch (Exception e) {
            if (Environment.current().isDev()) {
                // Ikke alle organisasjoner som brukes av Dolly finnes i Tenor, som Altinn bruker for å slå opp bedrifter i test. Må derfor tåle å feile for enkelte kall i dev
                LOG.warn("Feil ved kall til dialogporten: ", e);
            } else {
                throw new IllegalStateException("Kunne ikke opprette forespørsel i dialogporten, fikk feil " + e);
            }
        }
        LOG.info("Sluttfører task for opprettelse av sak i dialogporten for saksnummer {} og orgnr {}", forespørselDto.fagsystemSaksnummer(), forespørselDto.arbeidsgiver());
    }

    public static ProsessTaskData lagTask(UUID forespørselUuid) {
        var task = ProsessTaskData.forProsessTask(OpprettSakDialogportenTask.class);
        // Viktig at tasker som skal oppdatere forespørselentitet alle deler samme gruppe (forespørselUuid) så vi ikke får lås
        task.setGruppe(forespørselUuid.toString());
        task.setProperty(FORESPØRSEL_UUID, forespørselUuid.toString());
        return task;
    }
}
