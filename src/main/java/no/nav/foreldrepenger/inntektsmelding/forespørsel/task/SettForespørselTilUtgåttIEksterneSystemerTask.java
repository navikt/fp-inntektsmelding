package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

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
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenKlient;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "forespoersel.settUtgaattEksterneSystemer", maxFailedRuns = 3)
public class SettForespørselTilUtgåttIEksterneSystemerTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SettForespørselTilUtgåttIEksterneSystemerTask.class);

    public static final String KEY_FORESPOERSEL_UUID = "forespoerselUuid";
    public static final String KEY_SKAL_OPPDATERE_AG_NOTIFIKASJON = "skalOppdatereAgNotifikasjon";

    private ForespørselTjeneste forespørselTjeneste;
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    private PersonTjeneste personTjeneste;
    private DialogportenKlient dialogportenKlient;

    SettForespørselTilUtgåttIEksterneSystemerTask() {
        // CDI
    }

    @Inject
    public SettForespørselTilUtgåttIEksterneSystemerTask(ForespørselTjeneste forespørselTjeneste,
                                                          MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste,
                                                          PersonTjeneste personTjeneste,
                                                          DialogportenKlient dialogportenKlient) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
        this.personTjeneste = personTjeneste;
        this.dialogportenKlient = dialogportenKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(KEY_FORESPOERSEL_UUID));
        var skalOppdatereArbeidsgiverNotifikasjon = Boolean.parseBoolean(prosessTaskData.getPropertyValue(KEY_SKAL_OPPDATERE_AG_NOTIFIKASJON));

        LOG.info("Starter setting av forespørsel til utgått i eksterne systemer for forespørselUuid: {}", forespørselUuid);

        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel med uuid: " + forespørselUuid));

        if (forespørsel.arbeidsgiverNotifikasjonSakId() == null) {
            throw new IllegalStateException("Forespørsel mangler arbeidsgiverNotifikasjonSakId, kan ikke sette til utgått i eksterne systemer. Uuid: " + forespørselUuid);
        }

        if (skalOppdatereArbeidsgiverNotifikasjon) {
            Optional.ofNullable(forespørsel.oppgaveId())
                .ifPresent(oppgaveId -> minSideArbeidsgiverTjeneste.oppgaveUtgått(oppgaveId, OffsetDateTime.now()));
            minSideArbeidsgiverTjeneste.ferdigstillSak(forespørsel.arbeidsgiverNotifikasjonSakId(), false);
        }

        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(forespørsel.arbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT, forespørsel.førsteUttaksdato()));

        Optional.ofNullable(forespørsel.dialogportenUuid()).ifPresent(dialogUuid ->
            dialogportenKlient.settDialogTilUtgått(dialogUuid,
                lagSaksTittelForDialogporten(forespørsel.aktørId(), forespørsel.ytelseType())));

        LOG.info("Ferdig med setting av forespørsel til utgått i eksterne systemer for forespørselUuid: {}", forespørselUuid);
    }

    private String lagSaksTittelForDialogporten(AktørId aktørId, Ytelsetype ytelsetype) {
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype);
        return ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato());
    }

    public static ProsessTaskData opprettTask(UUID forespørselUuid,
                                              boolean skalOppdatereArbeidsgiverNotifikasjon) {
        var task = ProsessTaskData.forProsessTask(SettForespørselTilUtgåttIEksterneSystemerTask.class);
        task.setProperty(KEY_FORESPOERSEL_UUID, forespørselUuid.toString());
        task.setProperty(KEY_SKAL_OPPDATERE_AG_NOTIFIKASJON, String.valueOf(skalOppdatereArbeidsgiverNotifikasjon));
        return task;
    }
}
