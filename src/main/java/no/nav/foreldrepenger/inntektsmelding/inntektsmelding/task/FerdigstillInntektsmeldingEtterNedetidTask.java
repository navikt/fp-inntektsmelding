package no.nav.foreldrepenger.inntektsmelding.inntektsmelding.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektKontrollTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "ferdigstill.etter.nedetid")
public class FerdigstillInntektsmeldingEtterNedetidTask implements ProsessTaskHandler {
    public static final String KEY_INNTEKTSMELDING_ID = "inntektsmeldingId";
    private static final Logger LOG = LoggerFactory.getLogger(FerdigstillInntektsmeldingEtterNedetidTask.class);
    private InntektKontrollTjeneste inntektKontrollTjeneste;

    FerdigstillInntektsmeldingEtterNedetidTask() {
        // CDI
    }

    @Inject
    FerdigstillInntektsmeldingEtterNedetidTask(InntektKontrollTjeneste inntektKontrollTjeneste) {
        this.inntektKontrollTjeneste = inntektKontrollTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        LOG.info("Starter task ferdigstill inntektsmelding etter nedetid");
        var inntektsmeldingId = Long.parseLong(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_ID));
        inntektKontrollTjeneste.kontrollerInntektsmeldingEtterNedetid(inntektsmeldingId);
    }
}
