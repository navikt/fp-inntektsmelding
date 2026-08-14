package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
class OpprettDialogOgFerdigstillTaskTest {

    private static final String ORG_NUMMER = "999999999";
    private static final String AKTØR_ID = "9999999999999";
    private static final String SAK_ID = "sak-1";

    @Mock
    private ForespørselTjeneste forespørselTjeneste;
    @Mock
    private DialogportenTjeneste dialogportenTjeneste;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;

    private OpprettDialogOgFerdigstillTask task;

    @BeforeEach
    void setUp() {
        task = new OpprettDialogOgFerdigstillTask(forespørselTjeneste, dialogportenTjeneste, prosessTaskTjeneste);
    }

    @Test
    void skal_opprette_dialog_ferdigstille_og_opprette_ferdigstillingstasker() {
        var forespørselUuid = UUID.randomUUID();
        var inntektsmeldingUuid = UUID.randomUUID();
        var dialogportenUuid = UUID.randomUUID();
        var forespørsel = lagForespørselDto(forespørselUuid, SAK_ID, null);

        when(forespørselTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørsel));
        when(dialogportenTjeneste.opprettDialog(forespørsel)).thenReturn(dialogportenUuid);
        simulerAtDevToleranseKjørerHandlingen();

        var prosessTaskData = lagProsessTaskData(forespørselUuid, inntektsmeldingUuid);

        // Act
        task.doTask(prosessTaskData);

        // Assert - dialog opprettes siden den ikke fantes fra før
        verify(dialogportenTjeneste).opprettDialog(forespørsel);
        verify(forespørselTjeneste).setDialogportenUuid(forespørselUuid, dialogportenUuid);

        // Forespørsel ferdigstilles i databasen
        verify(forespørselTjeneste).ferdigstillForespørsel(SAK_ID);

        // Ferdigstillingstasker for sak og dialog opprettes med riktige properties
        var taskGruppeCaptor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(taskGruppeCaptor.capture());
        var taskGruppe = taskGruppeCaptor.getValue();

        var tasks = taskGruppe.getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
        assertThat(tasks).hasSize(2);
        var ferdigstillSakTask = tasks.getFirst();
        var ferdigstillDialogTask = tasks.getLast();
        assertThat(ferdigstillSakTask.taskType()).isEqualTo(TaskType.forProsessTask(FerdigstillSakTask.class));
        assertThat(ferdigstillSakTask.getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID)).isEqualTo(forespørselUuid.toString());
        assertThat(ferdigstillSakTask.getPropertyValue(FellesTaskProperties.KEY_LUKKE_AARSAK)).isEqualTo(LukkeÅrsak.ORDINÆR_INNSENDING.name());
        assertThat(ferdigstillSakTask.getPropertyValue(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID)).isEqualTo(inntektsmeldingUuid.toString());
        assertThat(ferdigstillSakTask.getPropertyValue(FerdigstillSakTask.KEY_ER_FØRSTEGANGSINNSENDING)).isEqualTo("true");
        assertThat(ferdigstillDialogTask.taskType()).isEqualTo(TaskType.forProsessTask(FerdigstillDialogTask.class));
        assertThat(ferdigstillDialogTask.getPropertyValue(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID)).isEqualTo(inntektsmeldingUuid.toString());
    }

    @Test
    void skal_ikke_opprette_dialog_på_nytt_dersom_den_allerede_finnes() {
        var forespørselUuid = UUID.randomUUID();
        var inntektsmeldingUuid = UUID.randomUUID();
        var eksisterendeDialogportenUuid = UUID.randomUUID();
        var forespørsel = lagForespørselDto(forespørselUuid, SAK_ID, eksisterendeDialogportenUuid);

        when(forespørselTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørsel));

        var prosessTaskData = lagProsessTaskData(forespørselUuid, inntektsmeldingUuid);

        // Act
        task.doTask(prosessTaskData);

        // Assert - idempotens: dialog opprettes ikke på nytt
        verify(dialogportenTjeneste, never()).opprettDialog(any());
        verify(forespørselTjeneste, never()).setDialogportenUuid(any(), any());
        verify(forespørselTjeneste).ferdigstillForespørsel(SAK_ID);
        verify(prosessTaskTjeneste).lagre(any(ProsessTaskGruppe.class));
    }

    @Test
    void skal_kaste_illegal_state_exception_dersom_sak_id_mangler() {
        var forespørselUuid = UUID.randomUUID();
        var inntektsmeldingUuid = UUID.randomUUID();
        var forespørsel = lagForespørselDto(forespørselUuid, null, UUID.randomUUID());

        when(forespørselTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørsel));

        var prosessTaskData = lagProsessTaskData(forespørselUuid, inntektsmeldingUuid);

        // Act & Assert
        assertThatThrownBy(() -> task.doTask(prosessTaskData))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(forespørselUuid.toString());

        // Ferdigstilling og ferdigstillingstasker skal ikke skje når sak mangler
        verify(forespørselTjeneste, never()).ferdigstillForespørsel(any());
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskGruppe.class));
    }

    @Test
    void skal_kaste_illegal_state_exception_dersom_forespørsel_ikke_finnes() {
        var forespørselUuid = UUID.randomUUID();
        when(forespørselTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.empty());

        var prosessTaskData = lagProsessTaskData(forespørselUuid, UUID.randomUUID());

        assertThatThrownBy(() -> task.doTask(prosessTaskData)).isInstanceOf(IllegalStateException.class);
    }

    private void simulerAtDevToleranseKjørerHandlingen() {
        Mockito.doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(dialogportenTjeneste).utførMotDialogportenMedDevToleranse(any());
    }

    private ProsessTaskData lagProsessTaskData(UUID forespørselUuid, UUID inntektsmeldingUuid) {
        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettDialogOgFerdigstillTask.class);
        prosessTaskData.setProperty(FellesTaskProperties.KEY_FORESPOERSEL_UUID, forespørselUuid.toString());
        prosessTaskData.setProperty(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID, inntektsmeldingUuid.toString());
        return prosessTaskData;
    }

    private ForespørselDto lagForespørselDto(UUID uuid, String sakId, UUID dialogportenUuid) {
        return ForespørselDto.builder()
            .uuid(uuid)
            .arbeidsgiver(Arbeidsgiver.fra(ORG_NUMMER))
            .aktørId(AktørId.fra(AKTØR_ID))
            .ytelseType(Ytelsetype.FORELDREPENGER)
            .status(ForespørselStatus.UNDER_BEHANDLING)
            .forespørselType(ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT)
            .førsteUttaksdato(LocalDate.now())
            .arbeidsgiverNotifikasjonSakId(sakId)
            .dialogportenUuid(dialogportenUuid)
            .build();
    }
}
