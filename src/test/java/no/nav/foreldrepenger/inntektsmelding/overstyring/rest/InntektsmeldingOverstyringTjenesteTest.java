package no.nav.foreldrepenger.inntektsmelding.overstyring.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import no.nav.vedtak.felles.prosesstask.api.TaskType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.task.SendTilJoarkTask;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingOverstyringTjenesteTest {

    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @InjectMocks
    private InntektsmeldingOverstyringTjeneste overstyringTjeneste;

    @Captor
    private ArgumentCaptor<ProsessTaskData> taskCaptor;

    @Test
    void skal_lagre_inntektsmelding_og_opprette_task_med_riktige_parametere() {
        var imDto = InntektsmeldingDto.builder().build();
        var saksnummer = Saksnummer.fra("SAK_123");
        var forventetImId = 42L;

        when(inntektsmeldingTjeneste.lagreInntektsmelding(any())).thenReturn(forventetImId);

        overstyringTjeneste.mottaOverstyrtInntektsmelding(imDto, saksnummer);

        verify(inntektsmeldingTjeneste).lagreInntektsmelding(imDto);
        verify(prosessTaskTjeneste).lagre(taskCaptor.capture());

        var expectedTaskType = TaskType.forProsessTask(SendTilJoarkTask.class);

        var task = taskCaptor.getValue();
        assertThat(task.getSaksnummer()).isEqualTo("SAK_123");
        assertThat(task.getPropertyValue(SendTilJoarkTask.KEY_INNTEKTSMELDING_ID)).isEqualTo("42");
        assertThat(task.taskType()).isEqualTo(expectedTaskType);
    }

    @Test
    void skal_sende_riktig_saksnummer_til_task() {
        var imDto = InntektsmeldingDto.builder().build();
        var saksnummer = Saksnummer.fra("FAGSAK_999");

        when(inntektsmeldingTjeneste.lagreInntektsmelding(any())).thenReturn(1L);

        overstyringTjeneste.mottaOverstyrtInntektsmelding(imDto, saksnummer);

        verify(prosessTaskTjeneste).lagre(taskCaptor.capture());

        assertThat(taskCaptor.getValue().getSaksnummer()).isEqualTo("FAGSAK_999");
    }

    @Test
    void skal_bruke_returnert_inntektsmelding_id_fra_lagring() {
        var imDto = InntektsmeldingDto.builder().build();
        var saksnummer = Saksnummer.fra("SAK_1");
        var imId = 12345L;

        when(inntektsmeldingTjeneste.lagreInntektsmelding(any())).thenReturn(imId);

        overstyringTjeneste.mottaOverstyrtInntektsmelding(imDto, saksnummer);

        verify(prosessTaskTjeneste).lagre(taskCaptor.capture());

        assertThat(taskCaptor.getValue().getPropertyValue(SendTilJoarkTask.KEY_INNTEKTSMELDING_ID))
            .isEqualTo(String.valueOf(imId));
    }
}
