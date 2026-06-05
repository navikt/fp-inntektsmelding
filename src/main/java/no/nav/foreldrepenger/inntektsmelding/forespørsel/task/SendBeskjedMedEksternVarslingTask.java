package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.net.URI;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTekster;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "forespoersel.sendBeskjedMedEksternVarsling", maxFailedRuns = 3)
public class SendBeskjedMedEksternVarslingTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SendBeskjedMedEksternVarslingTask.class);

    public static final String KEY_FORESPOERSEL_UUID = "forespoerselUuid";
    public static final String KEY_ARBEIDSGIVER_ORGNR = "arbeidsgiverOrgnr";

    private String inntektsmeldingSkjemaLenke;
    private ForespørselTjeneste forespørselTjeneste;
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private PersonTjeneste personTjeneste;

    SendBeskjedMedEksternVarslingTask() {
        // CDI
    }

    @Inject
    public SendBeskjedMedEksternVarslingTask(
        @KonfigVerdi(value = "inntektsmelding.skjema.lenke", defaultVerdi = "https://arbeidsgiver.nav.no/fp-im-dialog")
        String inntektsmeldingSkjemaLenke,
        ForespørselTjeneste forespørselTjeneste,
        MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste,
        OrganisasjonTjeneste organisasjonTjeneste,
        PersonTjeneste personTjeneste) {
        this.inntektsmeldingSkjemaLenke = inntektsmeldingSkjemaLenke;
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.personTjeneste = personTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(KEY_FORESPOERSEL_UUID));
        var arbeidsgiver = new Arbeidsgiver(prosessTaskData.getPropertyValue(KEY_ARBEIDSGIVER_ORGNR));

        LOG.info("Starter sending av beskjed med ekstern varsling for forespørselUuid: {}", forespørselUuid);

        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel med uuid: " + forespørselUuid));

        var merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.ytelseType());
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + forespørselUuid);
        var organisasjon = organisasjonTjeneste.finnOrganisasjon(arbeidsgiver);
        var person = personTjeneste.hentPersonInfoFraAktørId(forespørsel.aktørId(), forespørsel.ytelseType());
        var varselTekst = ForespørselTekster.lagVarselFraSaksbehandlerTekst(forespørsel.ytelseType(), organisasjon);
        var beskjedTekst = ForespørselTekster.lagBeskjedFraSaksbehandlerTekst(forespørsel.ytelseType(), person.mapFulltNavn());

        minSideArbeidsgiverTjeneste.sendNyBeskjedMedEksternVarsling(forespørselUuid.toString(),
            merkelapp,
            forespørselUuid.toString(),
            arbeidsgiver.orgnr(),
            beskjedTekst,
            varselTekst,
            skjemaUri);

        LOG.info("Ferdig med sending av beskjed med ekstern varsling for forespørselUuid: {}", forespørselUuid);
    }

    public static ProsessTaskData opprettTask(UUID forespørselUuid, Arbeidsgiver arbeidsgiver) {
        var task = ProsessTaskData.forProsessTask(SendBeskjedMedEksternVarslingTask.class);
        task.setProperty(KEY_FORESPOERSEL_UUID, forespørselUuid.toString());
        task.setProperty(KEY_ARBEIDSGIVER_ORGNR, arbeidsgiver.orgnr());
        return task;
    }
}
