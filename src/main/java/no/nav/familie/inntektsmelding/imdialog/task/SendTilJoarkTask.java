package no.nav.familie.inntektsmelding.imdialog.task;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.FpDokgenTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.joark.JoarkTjeneste;
import no.nav.familie.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "mottaInntektsmelding.oversendJoark")
public class SendTilJoarkTask implements ProsessTaskHandler {
    public static final String KEY_INNTEKTSMELDING_ID = "inntektsmeldingId";
    public static final String KEY_FORESPOERSEL_TYPE = "forespoerselType";
    private static final Logger LOG = LoggerFactory.getLogger(SendTilJoarkTask.class);
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;
    private InntektsmeldingXMLTjeneste inntektsmeldingXMLTjeneste;
    private FpDokgenTjeneste fpDokgenTjeneste;
    private JoarkTjeneste joarkTjeneste;

    SendTilJoarkTask() {
        // CDI
    }

    @Inject
    public SendTilJoarkTask(InntektsmeldingTjeneste inntektsmeldingTjeneste,
                            InntektsmeldingRepository inntektsmeldingRepository,
                            InntektsmeldingXMLTjeneste inntektsmeldingXMLTjeneste,
                            FpDokgenTjeneste fpDokgenTjeneste,
                            JoarkTjeneste joarkTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.inntektsmeldingXMLTjeneste = inntektsmeldingXMLTjeneste;
        this.fpDokgenTjeneste = fpDokgenTjeneste;
        this.joarkTjeneste = joarkTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var inntektsmeldingId = Long.parseLong(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_ID));
        var forespørselType = Optional.ofNullable(prosessTaskData.getPropertyValue(KEY_FORESPOERSEL_TYPE))
            .map(ForespørselType::valueOf)
            .orElse(ForespørselType.BESTILT_AV_FAGSYSTEM);
        var fagsysteSaksnummer = prosessTaskData.getSaksnummer();
        LOG.info("Starter task for oversending til joark for saksnummer {}", fagsysteSaksnummer);

        var inntektsmeldingDto = inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId);
        var inntektsmeldingEntitet = inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingId);
        var xml = inntektsmeldingXMLTjeneste.lagXMLAvInntektsmelding(inntektsmeldingDto);

        var pdf = fpDokgenTjeneste.mapDataOgGenererPdf(inntektsmeldingEntitet, forespørselType);

        LOG.debug("Genererte XML: {} og pdf av inntektsmeldingen, journalfører på sak: {}", xml, fagsysteSaksnummer);
        joarkTjeneste.journalførInntektsmelding(xml, inntektsmeldingEntitet, pdf, fagsysteSaksnummer);
        LOG.info("Sluttfører task oversendJoark");
    }
}
