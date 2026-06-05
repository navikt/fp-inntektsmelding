package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTekster;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.kvittering.PdfDokumentRest;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenKlient;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "forespoersel.oppdaterPortalerEndretIm", maxFailedRuns = 3)
public class OppdaterPortalerMedEndretInntektsmeldingTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OppdaterPortalerMedEndretInntektsmeldingTask.class);

    public static final String KEY_FORESPOERSEL_UUID = "forespoerselUuid";
    public static final String KEY_ARBEIDSGIVER_ORGNR = "arbeidsgiverOrgnr";
    public static final String KEY_INNTEKTSMELDING_UUID = "inntektsmeldingUuid";

    private String inntektsmeldingSkjemaLenke;
    private ForespørselTjeneste forespørselTjeneste;
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    private DialogportenKlient dialogportenKlient;

    OppdaterPortalerMedEndretInntektsmeldingTask() {
        // CDI
    }

    @Inject
    public OppdaterPortalerMedEndretInntektsmeldingTask(
        @KonfigVerdi(value = "inntektsmelding.skjema.lenke", defaultVerdi = "https://arbeidsgiver.nav.no/fp-im-dialog")
        String inntektsmeldingSkjemaLenke,
        ForespørselTjeneste forespørselTjeneste,
        MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste,
        DialogportenKlient dialogportenKlient) {
        this.inntektsmeldingSkjemaLenke = inntektsmeldingSkjemaLenke;
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
        this.dialogportenKlient = dialogportenKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(KEY_FORESPOERSEL_UUID));
        var arbeidsgiver = new Arbeidsgiver(prosessTaskData.getPropertyValue(KEY_ARBEIDSGIVER_ORGNR));
        var inntektsmeldingUuid = Optional.ofNullable(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_UUID))
            .map(UUID::fromString);

        LOG.info("Starter oppdatering av portaler med endret inntektsmelding for forespørselUuid: {}", forespørselUuid);

        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel med uuid: " + forespørselUuid));

        // Oppdater status i arbeidsgiverportalen
        inntektsmeldingUuid.ifPresent(imUuid -> {
            var merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.ytelseType());
            var beskjedTekst = ForespørselTekster.lagBeskjedOmOppdatertInntektsmelding();
            String url = new StringBuilder(inntektsmeldingSkjemaLenke)
                .append("/server/api")
                .append(PdfDokumentRest.INNTEKTSMELDING_FULL_PATH)
                .append("/")
                .append(imUuid).toString();
            minSideArbeidsgiverTjeneste.sendNyBeskjedMedKvittering(forespørsel.uuid().toString(),
                merkelapp,
                forespørsel.uuid().toString(),
                arbeidsgiver.orgnr(),
                beskjedTekst,
                URI.create(url));
        });

        // Oppdater status i altinn dialogporten
        var dialogUuid = forespørsel.dialogportenUuid();
        if (dialogUuid != null) {
            dialogportenKlient.oppdaterDialogMedEndretInntektsmelding(dialogUuid,
                arbeidsgiver,
                inntektsmeldingUuid);
        }

        LOG.info("Ferdig med oppdatering av portaler med endret inntektsmelding for forespørselUuid: {}", forespørselUuid);
    }

    public static ProsessTaskData opprettTask(UUID forespørselUuid,
                                              Arbeidsgiver arbeidsgiver,
                                              Optional<UUID> inntektsmeldingUuid) {
        var task = ProsessTaskData.forProsessTask(OppdaterPortalerMedEndretInntektsmeldingTask.class);
        task.setProperty(KEY_FORESPOERSEL_UUID, forespørselUuid.toString());
        task.setProperty(KEY_ARBEIDSGIVER_ORGNR, arbeidsgiver.orgnr());
        inntektsmeldingUuid.ifPresent(uuid -> task.setProperty(KEY_INNTEKTSMELDING_UUID, uuid.toString()));
        return task;
    }
}
