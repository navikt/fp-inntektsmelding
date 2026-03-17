package no.nav.foreldrepenger.inntektsmelding.inntektsmelding.task;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen.DokumentGeneratorTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.joark.JoarkTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
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
    private InntektsmeldingXMLTjeneste inntektsmeldingXMLTjeneste;
    private DokumentGeneratorTjeneste dokumentGeneratorTjeneste;
    private JoarkTjeneste joarkTjeneste;

    SendTilJoarkTask() {
        // CDI
    }

    @Inject
    SendTilJoarkTask(InntektsmeldingTjeneste inntektsmeldingTjeneste,
                     InntektsmeldingXMLTjeneste inntektsmeldingXMLTjeneste,
                     DokumentGeneratorTjeneste dokumentGeneratorTjeneste,
                     JoarkTjeneste joarkTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.inntektsmeldingXMLTjeneste = inntektsmeldingXMLTjeneste;
        this.dokumentGeneratorTjeneste = dokumentGeneratorTjeneste;
        this.joarkTjeneste = joarkTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var inntektsmeldingId = Long.parseLong(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_ID));
        var forespørselType = Optional.ofNullable(prosessTaskData.getPropertyValue(KEY_FORESPOERSEL_TYPE))
            .map(ForespørselType::valueOf)
            .orElse(ForespørselType.BESTILT_AV_FAGSYSTEM);
        var fagsysteSaksnummer = Saksnummer.fra(prosessTaskData.getSaksnummer());
        LOG.info("Starter task for oversending til joark for saksnummer {}", fagsysteSaksnummer);

        var inntektsmeldingDto = inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId);
        var xml = inntektsmeldingXMLTjeneste.lagXMLAvInntektsmelding(inntektsmeldingDto);
        var pdf = dokumentGeneratorTjeneste.mapDataOgGenererPdf(inntektsmeldingDto, forespørselType);
        LOG.debug("Genererte XML: {} og pdf av inntektsmeldingen, journalfører på sak: {}", xml, fagsysteSaksnummer.saksnummer());

        joarkTjeneste.journalførInntektsmelding(xml, inntektsmeldingDto, pdf, fagsysteSaksnummer);
        LOG.info("Sluttfører task oversendJoark");
    }
}
