package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTekster;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.kvittering.PdfDokumentRest;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenKlient;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "forespoersel.ferdigstillEksterneSystemer", maxFailedRuns = 3)
public class FerdigstillForespørselIEksterneSystemerTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FerdigstillForespørselIEksterneSystemerTask.class);
    private static final Environment ENV = Environment.current();

    public static final String KEY_FORESPOERSEL_UUID = "forespoerselUuid";
    public static final String KEY_ARBEIDSGIVER_ORGNR = "arbeidsgiverOrgnr";
    public static final String KEY_AKTOER_ID = "aktoerId";
    public static final String KEY_LUKKE_AARSAK = "lukkeAarsak";
    public static final String KEY_INNTEKTSMELDING_UUID = "inntektsmeldingUuid";
    public static final String KEY_ER_FORSTEGANGSINNSENDING = "erForstegangsinnsending";

    private String inntektsmeldingSkjemaLenke;
    private ForespørselTjeneste forespørselTjeneste;
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    private PersonTjeneste personTjeneste;
    private DialogportenKlient dialogportenKlient;

    FerdigstillForespørselIEksterneSystemerTask() {
        // CDI
    }

    @Inject
    public FerdigstillForespørselIEksterneSystemerTask(
        @KonfigVerdi(value = "inntektsmelding.skjema.lenke", defaultVerdi = "https://arbeidsgiver.nav.no/fp-im-dialog")
        String inntektsmeldingSkjemaLenke,
        ForespørselTjeneste forespørselTjeneste,
        MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste,
        PersonTjeneste personTjeneste,
        DialogportenKlient dialogportenKlient) {
        this.inntektsmeldingSkjemaLenke = inntektsmeldingSkjemaLenke;
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
        this.personTjeneste = personTjeneste;
        this.dialogportenKlient = dialogportenKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(KEY_FORESPOERSEL_UUID));
        var arbeidsgiver = new Arbeidsgiver(prosessTaskData.getPropertyValue(KEY_ARBEIDSGIVER_ORGNR));
        var aktørId = AktørId.fra(prosessTaskData.getPropertyValue(KEY_AKTOER_ID));
        var årsak = LukkeÅrsak.valueOf(prosessTaskData.getPropertyValue(KEY_LUKKE_AARSAK));
        var inntektsmeldingUuid = Optional.ofNullable(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_UUID))
            .map(UUID::fromString);
        var erFørstegangsinnsending = Boolean.parseBoolean(prosessTaskData.getPropertyValue(KEY_ER_FORSTEGANGSINNSENDING));

        LOG.info("Starter ferdigstilling av forespørsel i eksterne systemer for forespørselUuid: {}", forespørselUuid);

        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel med uuid: " + forespørselUuid));

        // Forespørselen må ha sakId for å kunne oppdatere eksterne systemer
        if (forespørsel.arbeidsgiverNotifikasjonSakId() == null) {
            throw new IllegalStateException("Forespørsel mangler arbeidsgiverNotifikasjonSakId, kan ikke ferdigstille i eksterne systemer. Uuid: " + forespørselUuid);
        }

        // Arbeidsgiverinitierte forespørsler har ingen oppgave
        if (forespørsel.oppgaveId() != null) {
            minSideArbeidsgiverTjeneste.oppgaveUtført(forespørsel.oppgaveId(), OffsetDateTime.now());
        }

        var erArbeidsgiverInitiertInntektsmelding = forespørsel.oppgaveId() == null;
        minSideArbeidsgiverTjeneste.ferdigstillSak(forespørsel.arbeidsgiverNotifikasjonSakId(), erArbeidsgiverInitiertInntektsmelding);

        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(forespørsel.arbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(årsak, forespørsel.førsteUttaksdato()));

        inntektsmeldingUuid.ifPresent(imUuid -> {
            var merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.ytelseType());
            var beskjedTekst = erFørstegangsinnsending
                               ? ForespørselTekster.lagBeskjedOmKvitteringFørsteInnsendingTekst()
                               : ForespørselTekster.lagBeskjedOmOppdatertInntektsmelding();
            String url = new StringBuilder(inntektsmeldingSkjemaLenke)
                .append("/server/api")
                .append(PdfDokumentRest.INNTEKTSMELDING_FULL_PATH)
                .append("/")
                .append(imUuid).toString();
            minSideArbeidsgiverTjeneste.sendNyBeskjedMedKvittering(forespørselUuid.toString(),
                merkelapp,
                forespørselUuid.toString(),
                arbeidsgiver.orgnr(),
                beskjedTekst,
                URI.create(url));
        });

        // Oppdaterer status i altinn dialogporten
        if (forespørsel.dialogportenUuid() != null) {
            if (ENV.isDev()) {
                try {
                    dialogportenKlient.ferdigstillDialog(forespørsel.dialogportenUuid(),
                        arbeidsgiver,
                        lagSaksTittelForDialogporten(aktørId, forespørsel.ytelseType()),
                        forespørsel.ytelseType(),
                        forespørsel.førsteUttaksdato(),
                        inntektsmeldingUuid,
                        årsak);
                } catch (Exception e) {
                    LOG.warn("Feil ved kall til dialogporten: ", e);
                }
            }
        }

        LOG.info("Ferdig med ferdigstilling av forespørsel i eksterne systemer for forespørselUuid: {}", forespørselUuid);
    }

    private String lagSaksTittelForDialogporten(AktørId aktørId, Ytelsetype ytelsetype) {
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype);
        return ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato());
    }

    public static ProsessTaskData opprettTask(UUID forespørselUuid,
                                              Arbeidsgiver arbeidsgiver,
                                              AktørId aktørId,
                                              LukkeÅrsak årsak,
                                              Optional<UUID> inntektsmeldingUuid,
                                              boolean erFørstegangsinnsending) {
        var task = ProsessTaskData.forProsessTask(FerdigstillForespørselIEksterneSystemerTask.class);
        task.setProperty(KEY_FORESPOERSEL_UUID, forespørselUuid.toString());
        task.setProperty(KEY_ARBEIDSGIVER_ORGNR, arbeidsgiver.orgnr());
        task.setProperty(KEY_AKTOER_ID, aktørId.getAktørId());
        task.setProperty(KEY_LUKKE_AARSAK, årsak.name());
        inntektsmeldingUuid.ifPresent(uuid -> task.setProperty(KEY_INNTEKTSMELDING_UUID, uuid.toString()));
        task.setProperty(KEY_ER_FORSTEGANGSINNSENDING, String.valueOf(erFørstegangsinnsending));
        return task;
    }
}
