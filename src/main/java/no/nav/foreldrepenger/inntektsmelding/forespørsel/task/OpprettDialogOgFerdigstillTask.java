package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Denne tasken brukes bare av Arbeidsgiverinitiert inntektsmelding da den oppretter sak i arbeidsgiverportalen
 * og deretter ferdigstiller disse. Vi kan ikke forsøke å ferdigstille dersom opprettelsen feiler, så derfor er
 * oppretter denne tasken ferdigstilling slik at vi vet ingenting har feilet
 */
@ApplicationScoped
@ProsessTask(value = "forespørsel.agi.dialog.opprett.ferdigstill")
public class OpprettDialogOgFerdigstillTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OpprettDialogOgFerdigstillTask.class);

    private ForespørselTjeneste forespørselTjeneste;
    private DialogportenTjeneste dialogportenTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    OpprettDialogOgFerdigstillTask() {
        // CDI
    }

    @Inject
    public OpprettDialogOgFerdigstillTask(ForespørselTjeneste forespørselTjeneste,
                                          DialogportenTjeneste dialogportenTjeneste,
                                          ProsessTaskTjeneste prosessTaskTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.dialogportenTjeneste = dialogportenTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID));
        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel " + forespørselUuid + " ved opprettelse av dialog"));

        if (forespørsel.dialogportenUuid() == null) {
            LOG.info("Oppretter dialog hos Dialogporten for forespørsel {}", forespørselUuid);
            dialogportenTjeneste.utførMotDialogportenMedDevToleranse(() -> {
                var dialogportenUuid = dialogportenTjeneste.opprettDialog(forespørsel);
                forespørselTjeneste.setDialogportenUuid(forespørselUuid, dialogportenUuid);
                LOG.info("Opprettet dialog {} hos Dialogporten for forespørsel {}", dialogportenUuid, forespørselUuid);
            });
        } else {
            // Idempotens: unngår å opprette en ny dialog dersom et tidligere (delvis) forsøk allerede har lykkes
            LOG.info("Dialog er allerede opprettet for forespørsel {}, går videre til ferdigstilling", forespørselUuid);
        }

        if (forespørsel.arbeidsgiverNotifikasjonSakId() == null) {
            throw new IllegalStateException("Finner ikke sakId for arbeidsgiverportal for forespørsel med uuid " + forespørselUuid + "Opprettelse av sak må være fullført før ferdigstilling.");
        }
        // oppdaterer status til FERDIG i databasen
        forespørselTjeneste.ferdigstillForespørsel(forespørsel.arbeidsgiverNotifikasjonSakId());

        var inntektsmeldingUuid = UUID.fromString(prosessTaskData.getPropertyValue(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID));

        //oppretter ferdigstillingstasker her siden de er avhengig av at forespørsel.sak.opprett og opprettDialog er kjørt ok
        var ferdigstillSakTask = ProsessTaskData.forProsessTask(FerdigstillSakTask.class);
        var erFørstegangsinnsending = true;
        ferdigstillSakTask.setProperty(FerdigstillSakTask.KEY_ER_FØRSTEGANGSINNSENDING, Boolean.toString(erFørstegangsinnsending));
        var ferdigstillDialogTask = ProsessTaskData.forProsessTask(FerdigstillDialogTask.class);

        var taskGruppe = new ProsessTaskGruppe();
        taskGruppe.setProperty(FellesTaskProperties.KEY_FORESPOERSEL_UUID, forespørselUuid.toString());
        taskGruppe.setProperty(FellesTaskProperties.KEY_LUKKE_AARSAK, LukkeÅrsak.ORDINÆR_INNSENDING.name());
        taskGruppe.setProperty(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID, inntektsmeldingUuid.toString());
        taskGruppe.addNesteSekvensiell(ferdigstillSakTask);
        taskGruppe.addNesteSekvensiell(ferdigstillDialogTask);
        prosessTaskTjeneste.lagre(taskGruppe);
    }
}
