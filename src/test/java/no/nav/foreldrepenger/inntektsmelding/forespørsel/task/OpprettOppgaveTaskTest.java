package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
class OpprettOppgaveTaskTest {
    @Mock
    private ForespørselTjeneste forespørselTjeneste;
    @Mock
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;

    @Test
    void retry_skal_lukke_oppgave_når_forespørselen_er_utgått_etter_versjonskonflikt() {
        var uuid = UUID.randomUUID();
        var aktiv = ForespørselDto.builder().uuid(uuid).status(ForespørselStatus.UNDER_BEHANDLING).build();
        var utgått = ForespørselDto.builder().uuid(uuid).status(ForespørselStatus.UTGÅTT).build();
        var taskData = lagTaskData(uuid);
        var task = new OpprettOppgaveTask(forespørselTjeneste, minSideArbeidsgiverTjeneste);
        when(forespørselTjeneste.hentForespørsel(uuid)).thenReturn(aktiv, utgått);
        when(minSideArbeidsgiverTjeneste.opprettOppgave(aktiv)).thenReturn("oppgave-1");
        doThrow(new OptimisticLockException()).when(forespørselTjeneste).setOppgaveId(uuid, "oppgave-1");

        assertThatThrownBy(() -> task.doTask(taskData)).isInstanceOf(OptimisticLockException.class);
        task.doTask(taskData);

        verify(minSideArbeidsgiverTjeneste).opprettOppgave(aktiv);
        verify(minSideArbeidsgiverTjeneste).settOppgaveTilUtgått(utgått);
        verifyNoMoreInteractions(minSideArbeidsgiverTjeneste);
    }

    @Test
    void feil_under_opprydding_skal_kastes_videre_for_nytt_forsøk() {
        var uuid = UUID.randomUUID();
        var utgått = ForespørselDto.builder().uuid(uuid).status(ForespørselStatus.UTGÅTT).build();
        when(forespørselTjeneste.hentForespørsel(uuid)).thenReturn(utgått);
        doThrow(new IllegalStateException("Fager er utilgjengelig"))
            .when(minSideArbeidsgiverTjeneste).settOppgaveTilUtgått(utgått);
        var task = new OpprettOppgaveTask(forespørselTjeneste, minSideArbeidsgiverTjeneste);
        var taskData = lagTaskData(uuid);

        assertThatThrownBy(() -> task.doTask(taskData)).isInstanceOf(IllegalStateException.class);
    }

    private static ProsessTaskData lagTaskData(UUID uuid) {
        var taskData = ProsessTaskData.forProsessTask(OpprettOppgaveTask.class);
        taskData.setProperty(FellesTaskProperties.KEY_FORESPOERSEL_UUID, uuid.toString());
        return taskData;
    }
}
