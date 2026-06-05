package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTekster;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenKlient;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
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
@ProsessTask(value = "forespoersel.opprettEksterneSystemer", maxFailedRuns = 3)
public class OpprettForespørselIEksterneSystemerTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OpprettForespørselIEksterneSystemerTask.class);
    private static final Environment ENV = Environment.current();

    public static final String KEY_FORESPOERSEL_UUID = "forespoerselUuid";
    public static final String KEY_ARBEIDSGIVER_ORGNR = "arbeidsgiverOrgnr";
    public static final String KEY_AKTOER_ID = "aktoerId";
    public static final String KEY_YTELSE_TYPE = "ytelseType";
    public static final String KEY_FORSTE_UTTAKSDATO = "forsteUttaksdato";

    private String inntektsmeldingSkjemaLenke;
    private ForespørselTjeneste forespørselTjeneste;
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private DialogportenKlient dialogportenKlient;

    OpprettForespørselIEksterneSystemerTask() {
        // CDI
    }

    @Inject
    public OpprettForespørselIEksterneSystemerTask(
        @KonfigVerdi(value = "inntektsmelding.skjema.lenke", defaultVerdi = "https://arbeidsgiver.nav.no/fp-im-dialog")
        String inntektsmeldingSkjemaLenke,
        ForespørselTjeneste forespørselTjeneste,
        MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste,
        PersonTjeneste personTjeneste,
        OrganisasjonTjeneste organisasjonTjeneste,
        DialogportenKlient dialogportenKlient) {
        this.inntektsmeldingSkjemaLenke = inntektsmeldingSkjemaLenke;
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.dialogportenKlient = dialogportenKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(KEY_FORESPOERSEL_UUID));
        var arbeidsgiver = new Arbeidsgiver(prosessTaskData.getPropertyValue(KEY_ARBEIDSGIVER_ORGNR));
        var aktørId = AktørId.fra(prosessTaskData.getPropertyValue(KEY_AKTOER_ID));
        var ytelsetype = Ytelsetype.valueOf(prosessTaskData.getPropertyValue(KEY_YTELSE_TYPE));
        var førsteUttaksdato = LocalDate.parse(prosessTaskData.getPropertyValue(KEY_FORSTE_UTTAKSDATO));

        LOG.info("Starter opprettelse av forespørsel i eksterne systemer for forespørselUuid: {}", forespørselUuid);

        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel med uuid: " + forespørselUuid));

        // Idempotency: skip MinSide opprettelse hvis sakId allerede er satt
        if (forespørsel.arbeidsgiverNotifikasjonSakId() == null) {
            opprettMinSideArbeidsgiverSakOgOppgave(forespørselUuid, arbeidsgiver, aktørId, ytelsetype, førsteUttaksdato);
        } else {
            LOG.info("SakId allerede satt for forespørsel {}, hopper over MinSide-opprettelse", forespørselUuid);
        }

        // Idempotency: skip Dialogporten opprettelse hvis dialogportenUuid allerede er satt
        if (forespørsel.dialogportenUuid() == null && ENV.isDev()) {
            try {
                opprettDialogportenDialog(forespørselUuid, arbeidsgiver, aktørId, ytelsetype, førsteUttaksdato);
            } catch (Exception e) {
                LOG.warn("Feil ved kall til dialogporten: ", e);
            }
        }

        LOG.info("Ferdig med opprettelse av forespørsel i eksterne systemer for forespørselUuid: {}", forespørselUuid);
    }

    private void opprettMinSideArbeidsgiverSakOgOppgave(UUID forespørselUuid,
                                                         Arbeidsgiver arbeidsgiver,
                                                         AktørId aktørId,
                                                         Ytelsetype ytelsetype,
                                                         LocalDate førsteUttaksdato) {
        var organisasjon = organisasjonTjeneste.finnOrganisasjon(arbeidsgiver);
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype);

        var merkelapp = ForespørselTekster.finnMerkelapp(ytelsetype);
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + forespørselUuid);
        var arbeidsgiverNotifikasjonSakId = minSideArbeidsgiverTjeneste.opprettSak(forespørselUuid.toString(),
            merkelapp,
            arbeidsgiver.orgnr(),
            ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato()),
            skjemaUri);

        var tilleggsinformasjon = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.ORDINÆR_INNSENDING, førsteUttaksdato);
        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(arbeidsgiverNotifikasjonSakId, tilleggsinformasjon);

        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(forespørselUuid, arbeidsgiverNotifikasjonSakId);

        String oppgaveId;
        try {
            oppgaveId = minSideArbeidsgiverTjeneste.opprettOppgave(forespørselUuid.toString(),
                merkelapp,
                forespørselUuid.toString(),
                arbeidsgiver.orgnr(),
                ForespørselTekster.lagOppgaveTekst(ytelsetype),
                ForespørselTekster.lagVarselTekst(ytelsetype, organisasjon),
                ForespørselTekster.lagPåminnelseTekst(ytelsetype, organisasjon),
                skjemaUri);
        } catch (Exception e) {
            minSideArbeidsgiverTjeneste.slettSak(arbeidsgiverNotifikasjonSakId);
            throw e;
        }
        forespørselTjeneste.setOppgaveId(forespørselUuid, oppgaveId);
    }

    private void opprettDialogportenDialog(UUID forespørselUuid,
                                           Arbeidsgiver arbeidsgiver,
                                           AktørId aktørId,
                                           Ytelsetype ytelsetype,
                                           LocalDate førsteUttaksdato) {
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype);
        var saksTittelDialog = ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato());

        var dialogPortenUuid = dialogportenKlient.opprettDialog(forespørselUuid,
            arbeidsgiver, saksTittelDialog, førsteUttaksdato, ytelsetype);

        var vasketDialogUuid = dialogPortenUuid.replace("\"", "");
        LOG.info("Mottok UUID {} fra dialogporten", vasketDialogUuid);
        forespørselTjeneste.setDialogportenUuid(forespørselUuid, UUID.fromString(vasketDialogUuid));
    }

    public static ProsessTaskData opprettTask(UUID forespørselUuid,
                                              Arbeidsgiver arbeidsgiver,
                                              AktørId aktørId,
                                              Ytelsetype ytelsetype,
                                              LocalDate førsteUttaksdato) {
        var task = ProsessTaskData.forProsessTask(OpprettForespørselIEksterneSystemerTask.class);
        task.setProperty(KEY_FORESPOERSEL_UUID, forespørselUuid.toString());
        task.setProperty(KEY_ARBEIDSGIVER_ORGNR, arbeidsgiver.orgnr());
        task.setProperty(KEY_AKTOER_ID, aktørId.getAktørId());
        task.setProperty(KEY_YTELSE_TYPE, ytelsetype.name());
        task.setProperty(KEY_FORSTE_UTTAKSDATO, førsteUttaksdato.toString());
        return task;
    }
}
