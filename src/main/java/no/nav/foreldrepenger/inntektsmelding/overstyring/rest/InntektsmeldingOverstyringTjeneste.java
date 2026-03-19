package no.nav.foreldrepenger.inntektsmelding.overstyring.rest;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;

import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.task.SendTilJoarkTask;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
public class InntektsmeldingOverstyringTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingOverstyringTjeneste.class);

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    InntektsmeldingOverstyringTjeneste() {
        // CDI proxy
    }

    @Inject
    public InntektsmeldingOverstyringTjeneste(InntektsmeldingTjeneste inntektsmeldingTjeneste, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public void mottaOverstyrtInntektsmelding(InntektsmeldingDto inntektsmeldingDto, Saksnummer saksnummer) {
        opprettTaskForSendTilJoark(inntektsmeldingTjeneste.lagreInntektsmelding(inntektsmeldingDto), saksnummer);
    }

    private void opprettTaskForSendTilJoark(Long imId, Saksnummer fagsystemSaksnummer) {
        var task = ProsessTaskData.forProsessTask(SendTilJoarkTask.class);
        task.setSaksnummer(fagsystemSaksnummer.saksnummer());
        task.setProperty(SendTilJoarkTask.KEY_INNTEKTSMELDING_ID, imId.toString());
        prosessTaskTjeneste.lagre(task);
        LOG.info("Opprettet task for oversending til joark");
    }
}
