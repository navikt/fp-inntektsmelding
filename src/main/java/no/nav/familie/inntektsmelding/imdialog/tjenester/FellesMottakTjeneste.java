package no.nav.familie.inntektsmelding.imdialog.tjenester;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.task.SendTilJoarkTask;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FellesMottakTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FellesMottakTjeneste.class);
    private InntektsmeldingRepository inntektsmeldingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    FellesMottakTjeneste() {
        //CDI
    }

    @Inject
    public FellesMottakTjeneste(InntektsmeldingRepository inntektsmeldingRepository, ProsessTaskTjeneste prosessTaskTjeneste,
                                ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    public InntektsmeldingEntitet lagreOgJournalførInntektsmelding(InntektsmeldingEntitet imEnitet, ForespørselEntitet forespørselEnitet) {
        var imId = lagreOgLagJournalførTask(imEnitet, forespørselEnitet);
        return inntektsmeldingRepository.hentInntektsmelding(imId);
    }

    private Long lagreOgLagJournalførTask(InntektsmeldingEntitet entitet, ForespørselEntitet forespørsel) {
        LOG.info("Lagrer inntektsmelding for forespørsel {}", forespørsel.getUuid());
        var imId = inntektsmeldingRepository.lagreInntektsmelding(entitet);
        opprettTaskForSendTilJoark(imId, forespørsel);
        return imId;
    }

    private void opprettTaskForSendTilJoark(Long imId, ForespørselEntitet forespørsel) {
        var task = ProsessTaskData.forProsessTask(SendTilJoarkTask.class);
        forespørsel.getFagsystemSaksnummer().ifPresent(task::setSaksnummer);
        task.setProperty(SendTilJoarkTask.KEY_INNTEKTSMELDING_ID, imId.toString());
        task.setProperty(SendTilJoarkTask.KEY_FORESPOERSEL_TYPE, forespørsel.getForespørselType().toString());
        prosessTaskTjeneste.lagre(task);
        LOG.info("Opprettet task for oversending til joark");
    }

    public void behandlerForespørsel(ForespørselEntitet forespørselEntitet, Optional<UUID> imId) {
        var orgnummer = new OrganisasjonsnummerDto(forespørselEntitet.getOrganisasjonsnummer());
        //Ferdigstiller forespørsel hvis den ikke er ferdig fra før
        if (!forespørselEntitet.getStatus().equals(ForespørselStatus.FERDIG)) {
            var aktørId = new AktørIdEntitet(forespørselEntitet.getAktørId().getAktørId());
            var ferdigstiltForespørsel = forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselEntitet.getUuid(), aktørId, orgnummer,
                forespørselEntitet.getFørsteUttaksdato(), LukkeÅrsak.ORDINÆR_INNSENDING, imId);
            MetrikkerTjeneste.loggForespørselLukkIntern(ferdigstiltForespørsel);
        } else {
            //legger inn oppdatert inntektsmelding i portaler
            forespørselBehandlingTjeneste.oppdaterPortalerMedEndretInntektsmelding(forespørselEntitet, imId, orgnummer);
        }
    }
}
