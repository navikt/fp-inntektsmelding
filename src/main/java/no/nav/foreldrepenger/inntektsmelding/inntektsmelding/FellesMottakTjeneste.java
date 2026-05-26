package no.nav.foreldrepenger.inntektsmelding.inntektsmelding;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.task.FerdigstillInntektsmeldingEtterNedetidTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.task.SendTilJoarkTask;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class FellesMottakTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FellesMottakTjeneste.class);
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    FellesMottakTjeneste() {
        //CDI
    }

    @Inject
    public FellesMottakTjeneste(InntektsmeldingTjeneste inntektsmeldingTjeneste, ProsessTaskTjeneste prosessTaskTjeneste,
                                ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    public InntektsmeldingDto lagreOgJournalførInntektsmelding(InntektsmeldingDto inntektsmelding, ForespørselDto forespørsel) {
        LOG.info("Lagrer inntektsmelding for forespørsel {}", forespørsel.uuid());
        var imId = inntektsmeldingTjeneste.lagreInntektsmelding(inntektsmelding, forespørsel.uuid());
        opprettTaskForSendTilJoark(imId, forespørsel);
        return inntektsmeldingTjeneste.hentInntektsmelding(imId);
    }

    public void opprettTaskForSendTilJoark(Long imId, ForespørselDto forespørsel) {
        var task = ProsessTaskData.forProsessTask(SendTilJoarkTask.class);
        Optional.ofNullable(forespørsel.fagsystemSaksnummer()).map(Saksnummer::saksnummer).ifPresent(task::setSaksnummer);
        task.setProperty(SendTilJoarkTask.KEY_INNTEKTSMELDING_ID, imId.toString());
        task.setProperty(SendTilJoarkTask.KEY_FORESPOERSEL_TYPE, forespørsel.forespørselType().toString());
        prosessTaskTjeneste.lagre(task);
        LOG.info("Opprettet task for oversending til joark");
    }

    public void lagreIMOgOpprettTaskForEtterkontroll (InntektsmeldingDto inntektsmelding, ForespørselDto forespørsel) {
        var lagretIMId = inntektsmeldingTjeneste.lagreInntektsmelding(inntektsmelding, forespørsel.uuid());
        var task = ProsessTaskData.forProsessTask(FerdigstillInntektsmeldingEtterNedetidTask.class);
        task.setProperty(FerdigstillInntektsmeldingEtterNedetidTask.KEY_INNTEKTSMELDING_ID, lagretIMId.toString());
        prosessTaskTjeneste.lagre(task);
        LOG.info("Opprettet task for etterkontroll av inntektsmelding");
    }

    public void ferdigstillOgOppdaterEksterneSystemer(ForespørselDto forespørsel, Optional<UUID> imId) {
        var orgnummer = forespørsel.arbeidsgiver();
        //Ferdigstiller forespørsel hvis den ikke er ferdig fra før
        if (!ForespørselStatus.FERDIG.equals(forespørsel.status())) {
            var aktørId = forespørsel.aktørId();
            var ferdigstiltForespørsel = forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørsel.uuid(), aktørId, orgnummer,
                forespørsel.førsteUttaksdato(), LukkeÅrsak.ORDINÆR_INNSENDING, imId);

            MetrikkerTjeneste.loggForespørselLukkIntern(ferdigstiltForespørsel);
        } else {
            //legger inn oppdatert inntektsmelding i portaler
            forespørselBehandlingTjeneste.oppdaterPortalerMedEndretInntektsmelding(forespørsel, imId, orgnummer);
        }
    }
}
