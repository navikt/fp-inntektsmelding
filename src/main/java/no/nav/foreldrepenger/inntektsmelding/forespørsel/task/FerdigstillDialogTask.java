package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
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
@ProsessTask(value = "forespørsel.ferdigstillDialog")
public class FerdigstillDialogTask implements ProsessTaskHandler {
    public static final String KEY_LUKKE_AARSAK = "lukkeAarsak";
    private static final Logger LOG = LoggerFactory.getLogger(FerdigstillDialogTask.class);

    private ForespørselTjeneste forespørselTjeneste;
    private DialogportenTjeneste dialogportenTjeneste;

    FerdigstillDialogTask() {
        // CDI
    }

    @Inject
    public FerdigstillDialogTask(ForespørselTjeneste forespørselTjeneste, DialogportenTjeneste dialogportenTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.dialogportenTjeneste = dialogportenTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID));
        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel " + forespørselUuid + " ved ferdigstilling av dialog"));

        var årsak = LukkeÅrsak.valueOf(prosessTaskData.getPropertyValue(KEY_LUKKE_AARSAK));
        var inntektsmeldingUuid = Optional.ofNullable(prosessTaskData.getPropertyValue(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID)).map(UUID::fromString);

        LOG.info("Ferdigstiller dialog hos Dialogporten for forespørsel {}", forespørselUuid);
        dialogportenTjeneste.utførMotDialogportenMedDevToleranse(() -> dialogportenTjeneste.ferdigstillDialog(forespørsel, årsak, inntektsmeldingUuid));
        LOG.info("Ferdigstilte dialog hos Dialogporten for forespørsel {}", forespørselUuid);
    }
}
