package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenTjeneste;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "ferdigstill.sak.dialogporten")
public class FerdigstillSakDialogportenTask implements ProsessTaskHandler {
    private static final String FORESPØRSEL_UUID = "forespørselUuid";
    private static final String INNTEKTSMELDING_UUID = "inntektsmeldingUuid"; // Nullable, da inntektsmelding kan komme fra eksternt system
    private static final String LUKKE_ÅRSAK = "lukkeÅrsak"; // Kan denne utledes fra forespørsel istedenfor å sendes med?
    private static final Logger LOG = LoggerFactory.getLogger(FerdigstillSakDialogportenTask.class);
    private DialogportenTjeneste dialogportenTjeneste;
    private ForespørselTjeneste forespørselTjeneste;

    FerdigstillSakDialogportenTask() {
        // CDI
    }

    @Inject
    FerdigstillSakDialogportenTask(DialogportenTjeneste dialogportenTjeneste,
                                   ForespørselTjeneste forespørselTjeneste) {
        this.dialogportenTjeneste = dialogportenTjeneste;
        this.forespørselTjeneste = forespørselTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FORESPØRSEL_UUID));
        var inntektsmeldingUuid = Optional.ofNullable(prosessTaskData.getPropertyValue(INNTEKTSMELDING_UUID)).map(UUID::fromString);
        var lukkeÅrsak = LukkeÅrsak.valueOf(prosessTaskData.getPropertyValue(LUKKE_ÅRSAK));
        var forespørselDto = forespørselTjeneste.hentForespørsel(forespørselUuid).orElseThrow();
        LOG.info("Starter task for å ferdigstille sak i dialogporten for saksnummer {} og orgnr {}", forespørselDto.fagsystemSaksnummer(), forespørselDto.arbeidsgiver());
        dialogportenTjeneste.ferdigstillDialog(forespørselDto, inntektsmeldingUuid, lukkeÅrsak);
        LOG.info("Sluttfører task for å ferdigstille sak i dialogporten for saksnummer {} og orgnr {}", forespørselDto.fagsystemSaksnummer(), forespørselDto.arbeidsgiver());
    }

    public static ProsessTaskData lagTask(UUID forespørselUuid, LukkeÅrsak lukkeÅrsak, Optional<UUID> inntektsmeldingUuid) {
        var task = ProsessTaskData.forProsessTask(FerdigstillSakDialogportenTask.class);
        // Viktig at tasker som skal oppdatere forespørselentitet alle deler samme gruppe (forespørselUuid) så vi ikke får lås
        task.setGruppe(forespørselUuid.toString());
        task.setProperty(FORESPØRSEL_UUID, forespørselUuid.toString());
        task.setProperty(LUKKE_ÅRSAK, lukkeÅrsak.toString());
        inntektsmeldingUuid.ifPresent(imUUid -> task.setProperty(INNTEKTSMELDING_UUID, inntektsmeldingUuid.toString()));
        return task;
    }
}
