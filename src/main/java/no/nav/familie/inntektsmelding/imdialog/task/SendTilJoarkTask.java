package no.nav.familie.inntektsmelding.imdialog.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.koder.ForespørselType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.FpDokgenTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.joark.JoarkTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import java.util.Optional;

@ApplicationScoped
@ProsessTask(value = "mottaInntektsmelding.oversendJoark")
public class SendTilJoarkTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SendTilJoarkTask.class);
    public static final String KEY_INNTEKTSMELDING_ID = "inntektsmeldingId";
    public static final String KEY_FORESØRSEL_TYPE = "forespørselType";

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private InntektsmeldingXMLTjeneste inntektsmeldingXMLTjeneste;
    private FpDokgenTjeneste fpDokgenTjeneste;
    private JoarkTjeneste joarkTjeneste;

    SendTilJoarkTask() {
        // CDI
    }

    @Inject
    public SendTilJoarkTask(InntektsmeldingTjeneste inntektsmeldingTjeneste,
                            InntektsmeldingXMLTjeneste inntektsmeldingXMLTjeneste,
                            FpDokgenTjeneste fpDokgenTjeneste,
                            JoarkTjeneste joarkTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.inntektsmeldingXMLTjeneste = inntektsmeldingXMLTjeneste;
        this.fpDokgenTjeneste = fpDokgenTjeneste;
        this.joarkTjeneste = joarkTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var inntektsmeldingId = Integer.parseInt(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_ID));
        var forespørselType = Optional.ofNullable(prosessTaskData.getPropertyValue(KEY_FORESØRSEL_TYPE)).map(ForespørselType::valueOf).orElseThrow();
        var fagsysteSaksnummer = prosessTaskData.getSaksnummer();
        LOG.info("Starter task for oversending til joark for saksnummer {}", fagsysteSaksnummer);

        var inntektsmelding = inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId);
        var xml = inntektsmeldingXMLTjeneste.lagXMLAvInntektsmelding(inntektsmelding);

        var pdf = fpDokgenTjeneste.mapDataOgGenererPdf(inntektsmelding, forespørselType);

        LOG.debug("Genererte XML: {} og pdf av inntektsmeldingen, journalfører på sak: {}", xml, fagsysteSaksnummer);
        joarkTjeneste.journalførInntektsmelding(xml, inntektsmelding, pdf, fagsysteSaksnummer);
        LOG.info("Sluttfører task oversendJoark");
    }
}
