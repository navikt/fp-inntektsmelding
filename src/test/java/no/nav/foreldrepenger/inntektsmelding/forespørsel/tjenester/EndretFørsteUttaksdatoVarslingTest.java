package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.FellesTaskProperties;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OppdaterDialogMedEndretFørsteUttaksdatoTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OppdaterSakMedEndretFørsteUttaksdatoTask;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
class EndretFørsteUttaksdatoVarslingTest {
    private static final LocalDate TIDLIGERE_DATO = LocalDate.of(2026, 9, 1);
    private static final LocalDate NY_DATO = LocalDate.of(2026, 9, 8);
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2026, 8, 1);
    private static final UUID FORESPØRSEL_UUID = UUID.randomUUID();
    private static final UUID DIALOG_UUID = UUID.randomUUID();
    private static final AktørId AKTØR_ID = AktørId.fra("1234567891234");
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.fra("974760673");
    private static final Saksnummer SAKSNUMMER = Saksnummer.fra("123");

    @Mock
    private ForespørselTjeneste forespørselTjeneste;
    @Mock
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    @Mock
    private DialogportenTjeneste dialogportenTjeneste;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;

    private ForespørselBehandlingTjeneste behandlingTjeneste;

    @BeforeEach
    void setUp() {
        behandlingTjeneste = new ForespørselBehandlingTjeneste(forespørselTjeneste, minSideArbeidsgiverTjeneste,
            dialogportenTjeneste, prosessTaskTjeneste);
    }

    @Test
    void skal_lagre_to_tasks_etter_datooppdatering_uten_synkrone_eksterne_kall() {
        var eksisterende = forespørsel(TIDLIGERE_DATO, DIALOG_UUID);
        when(forespørselTjeneste.finnArbeidsgiversÅpneForespørslerPåSak(SAKSNUMMER, ARBEIDSGIVER))
            .thenReturn(Optional.of(eksisterende));

        oppdaterDatoer(NY_DATO, SKJÆRINGSTIDSPUNKT);

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        var rekkefølge = inOrder(forespørselTjeneste, prosessTaskTjeneste);
        rekkefølge.verify(forespørselTjeneste).oppdaterFørsteUttaksdatoOgSkjæringstidspunkt(eksisterende, NY_DATO, SKJÆRINGSTIDSPUNKT);
        rekkefølge.verify(prosessTaskTjeneste).lagre(captor.capture());
        var tasks = captor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).taskType()).isEqualTo(TaskType.forProsessTask(OppdaterSakMedEndretFørsteUttaksdatoTask.class));
        assertThat(tasks.get(1).taskType()).isEqualTo(TaskType.forProsessTask(OppdaterDialogMedEndretFørsteUttaksdatoTask.class));
        assertThat(captor.getValue().getTasks()).extracting(ProsessTaskGruppe.Entry::sekvens).containsExactly("1", "2");
        assertThat(tasks).allSatisfy(task -> {
            assertThat(task.getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID)).isEqualTo(FORESPØRSEL_UUID.toString());
            assertThat(task.getProperties()).hasSize(task.harCallId() ? 2 : 1);
        });
        verifyNoInteractions(minSideArbeidsgiverTjeneste, dialogportenTjeneste);
    }

    @Test
    void skal_ikke_varsle_når_bare_skjæringstidspunkt_endres() {
        var eksisterende = forespørsel(TIDLIGERE_DATO, DIALOG_UUID);
        when(forespørselTjeneste.finnArbeidsgiversÅpneForespørslerPåSak(SAKSNUMMER, ARBEIDSGIVER))
            .thenReturn(Optional.of(eksisterende));

        oppdaterDatoer(TIDLIGERE_DATO, SKJÆRINGSTIDSPUNKT.plusDays(1));

        verify(forespørselTjeneste).oppdaterFørsteUttaksdatoOgSkjæringstidspunkt(eksisterende, TIDLIGERE_DATO,
            SKJÆRINGSTIDSPUNKT.plusDays(1));
        verifyNoInteractions(prosessTaskTjeneste, minSideArbeidsgiverTjeneste, dialogportenTjeneste);
    }

    @Test
    void skal_ikke_varsle_når_datoene_er_uendret() {
        when(forespørselTjeneste.finnArbeidsgiversÅpneForespørslerPåSak(SAKSNUMMER, ARBEIDSGIVER))
            .thenReturn(Optional.of(forespørsel(TIDLIGERE_DATO, DIALOG_UUID)));

        oppdaterDatoer(TIDLIGERE_DATO, SKJÆRINGSTIDSPUNKT);

        verify(forespørselTjeneste, never()).oppdaterFørsteUttaksdatoOgSkjæringstidspunkt(any(), any(), any());
        verifyNoInteractions(prosessTaskTjeneste, minSideArbeidsgiverTjeneste, dialogportenTjeneste);
    }

    @Test
    void skal_ikke_opprette_tasks_hvis_lokal_datooppdatering_feiler() {
        var eksisterende = forespørsel(TIDLIGERE_DATO, DIALOG_UUID);
        when(forespørselTjeneste.finnArbeidsgiversÅpneForespørslerPåSak(SAKSNUMMER, ARBEIDSGIVER))
            .thenReturn(Optional.of(eksisterende));
        var feil = new IllegalStateException("Lagring feilet");
        doThrow(feil).when(forespørselTjeneste).oppdaterFørsteUttaksdatoOgSkjæringstidspunkt(eksisterende, NY_DATO, SKJÆRINGSTIDSPUNKT);

        assertThatThrownBy(() -> oppdaterDatoer(NY_DATO, SKJÆRINGSTIDSPUNKT)).isSameAs(feil);

        verifyNoInteractions(prosessTaskTjeneste, minSideArbeidsgiverTjeneste, dialogportenTjeneste);
    }

    @Test
    void skal_varsle_om_første_uttaksdato_når_begge_datoene_endres() {
        var eksisterende = forespørsel(TIDLIGERE_DATO, DIALOG_UUID);
        var nyttSkjæringstidspunkt = SKJÆRINGSTIDSPUNKT.plusDays(1);
        when(forespørselTjeneste.finnArbeidsgiversÅpneForespørslerPåSak(SAKSNUMMER, ARBEIDSGIVER))
            .thenReturn(Optional.of(eksisterende));

        oppdaterDatoer(NY_DATO, nyttSkjæringstidspunkt);

        verify(forespørselTjeneste).oppdaterFørsteUttaksdatoOgSkjæringstidspunkt(eksisterende, NY_DATO, nyttSkjæringstidspunkt);
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        assertThat(captor.getValue().getTasks()).hasSize(2).allSatisfy(entry ->
            assertThat(entry.task().getProperties().values()).doesNotContain(nyttSkjæringstidspunkt.toString()));
    }

    @Test
    void skal_varsle_når_tidligere_uttaksdato_mangler() {
        when(forespørselTjeneste.finnArbeidsgiversÅpneForespørslerPåSak(SAKSNUMMER, ARBEIDSGIVER))
            .thenReturn(Optional.of(forespørsel(null, DIALOG_UUID)));

        oppdaterDatoer(NY_DATO, SKJÆRINGSTIDSPUNKT);

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        var tasks = captor.getValue().getTasks();
        assertThat(tasks).hasSize(2).allSatisfy(entry -> {
            assertThat(entry.task().getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID)).isEqualTo(FORESPØRSEL_UUID.toString());
            assertThat(entry.task().getProperties()).hasSize(entry.task().harCallId() ? 2 : 1);
        });
    }

    @Test
    void skal_opprette_tasks_for_hver_datoendring() {
        when(forespørselTjeneste.finnArbeidsgiversÅpneForespørslerPåSak(SAKSNUMMER, ARBEIDSGIVER))
            .thenReturn(Optional.of(forespørsel(TIDLIGERE_DATO, DIALOG_UUID)))
            .thenReturn(Optional.of(forespørsel(NY_DATO, DIALOG_UUID)));

        oppdaterDatoer(NY_DATO, SKJÆRINGSTIDSPUNKT);
        oppdaterDatoer(TIDLIGERE_DATO, SKJÆRINGSTIDSPUNKT);

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste, times(2)).lagre(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(gruppe -> assertThat(gruppe.getTasks()).hasSize(2));
    }

    @Test
    void sak_task_skal_bruke_siste_historiske_dato_og_gjeldende_dato() {
        var gjeldende = forespørsel(NY_DATO.plusDays(1), DIALOG_UUID);
        when(forespørselTjeneste.hentForespørsel(FORESPØRSEL_UUID)).thenReturn(gjeldende);
        var task = new OppdaterSakMedEndretFørsteUttaksdatoTask(forespørselTjeneste, minSideArbeidsgiverTjeneste);
        var data = taskData();

        task.doTask(data);
        task.doTask(data);

        verify(minSideArbeidsgiverTjeneste, times(2))
            .sendBeskjedOmEndretFørsteUttaksdato(gjeldende, TIDLIGERE_DATO, gjeldende.førsteUttaksdato());
        verifyNoInteractions(dialogportenTjeneste);
    }

    @Test
    void dialog_task_skal_bruke_siste_historiske_dato_og_gjeldende_dato() {
        var gjeldende = forespørsel(NY_DATO.plusDays(1), DIALOG_UUID);
        when(forespørselTjeneste.hentForespørsel(FORESPØRSEL_UUID)).thenReturn(gjeldende);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(dialogportenTjeneste).utførMotDialogportenMedDevToleranse(any(Runnable.class));

        new OppdaterDialogMedEndretFørsteUttaksdatoTask(forespørselTjeneste, dialogportenTjeneste).doTask(taskData());

        verify(dialogportenTjeneste).oppdaterDialogMedEndretFørsteUttaksdato(gjeldende, TIDLIGERE_DATO, gjeldende.førsteUttaksdato());
        verifyNoInteractions(minSideArbeidsgiverTjeneste);
    }

    @Test
    void manglende_dialog_skal_feile_for_retry_også_i_dev() {
        when(forespørselTjeneste.hentForespørsel(FORESPØRSEL_UUID)).thenReturn(forespørsel(NY_DATO, null));
        var task = new OppdaterDialogMedEndretFørsteUttaksdatoTask(forespørselTjeneste, dialogportenTjeneste);

        assertThatThrownBy(() -> task.doTask(taskData())).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(FORESPØRSEL_UUID.toString());
        verifyNoInteractions(dialogportenTjeneste);
    }

    @Test
    void manglende_forespørsel_skal_feile_i_begge_tasks() {
        var feil = new IllegalStateException("Finner ikke forespørsel " + FORESPØRSEL_UUID);
        when(forespørselTjeneste.hentForespørsel(FORESPØRSEL_UUID)).thenThrow(feil);
        var sakTask = new OppdaterSakMedEndretFørsteUttaksdatoTask(forespørselTjeneste, minSideArbeidsgiverTjeneste);
        var dialogTask = new OppdaterDialogMedEndretFørsteUttaksdatoTask(forespørselTjeneste, dialogportenTjeneste);

        assertThatThrownBy(() -> sakTask.doTask(taskData())).isSameAs(feil);
        assertThatThrownBy(() -> dialogTask.doTask(taskData())).isSameAs(feil);
        verifyNoInteractions(minSideArbeidsgiverTjeneste, dialogportenTjeneste);
    }

    private void oppdaterDatoer(LocalDate nyFørsteUttaksdato, LocalDate nyttSkjæringstidspunkt) {
        behandlingTjeneste.håndterInnkommendeForespørselNy(nyttSkjæringstidspunkt, Ytelsetype.FORELDREPENGER,
            AKTØR_ID, ARBEIDSGIVER, SAKSNUMMER, nyFørsteUttaksdato);
    }

    private static ForespørselDto forespørsel(LocalDate førsteUttaksdato, UUID dialogUuid) {
        return ForespørselDto.builder().uuid(FORESPØRSEL_UUID).aktørId(AKTØR_ID).arbeidsgiver(ARBEIDSGIVER)
            .ytelseType(Ytelsetype.FORELDREPENGER).status(ForespørselStatus.UNDER_BEHANDLING)
            .forespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM).fagsystemSaksnummer(SAKSNUMMER)
            .skjæringstidspunkt(SKJÆRINGSTIDSPUNKT).førsteUttaksdato(førsteUttaksdato)
            .leggTilEndringer(List.of(
                new ForespørselEndringHistorikkDto(SKJÆRINGSTIDSPUNKT, TIDLIGERE_DATO, NY_DATO.atStartOfDay()),
                new ForespørselEndringHistorikkDto(SKJÆRINGSTIDSPUNKT, TIDLIGERE_DATO.minusDays(1), TIDLIGERE_DATO.atStartOfDay())))
            .arbeidsgiverNotifikasjonSakId("1").dialogportenUuid(dialogUuid).build();
    }

    private static ProsessTaskData taskData() {
        var data = ProsessTaskData.forProsessTask(OppdaterSakMedEndretFørsteUttaksdatoTask.class);
        data.setProperty(FellesTaskProperties.KEY_FORESPOERSEL_UUID, FORESPØRSEL_UUID.toString());
        return data;
    }
}
