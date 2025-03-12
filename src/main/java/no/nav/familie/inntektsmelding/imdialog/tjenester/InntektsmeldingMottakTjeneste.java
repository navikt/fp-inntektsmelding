package no.nav.familie.inntektsmelding.imdialog.tjenester;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.imdialog.task.SendTilJoarkTask;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class InntektsmeldingMottakTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingMottakTjeneste.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    InntektsmeldingMottakTjeneste() {
    }

    @Inject
    public InntektsmeldingMottakTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                         InntektsmeldingRepository inntektsmeldingRepository,
                                         ProsessTaskTjeneste prosessTaskTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public InntektsmeldingResponseDto mottaInntektsmelding(SendInntektsmeldingRequestDto mottattInntektsmeldingDto) {
        var forespørselEntitet = forespørselBehandlingTjeneste.hentForespørsel(mottattInntektsmeldingDto.foresporselUuid())
            .orElseThrow(() -> new IllegalStateException("Mangler forespørsel entitet"));

        if (ForespørselStatus.UTGÅTT.equals(forespørselEntitet.getStatus())) {
            throw new IllegalStateException("Kan ikke motta nye inntektsmeldinger på utgåtte forespørsler");
        }

        var aktorId = new AktørIdEntitet(mottattInntektsmeldingDto.aktorId().id());
        var orgnummer = new OrganisasjonsnummerDto(mottattInntektsmeldingDto.arbeidsgiverIdent().ident());
        var entitet = InntektsmeldingMapper.mapTilEntitet(mottattInntektsmeldingDto);
        var imId = lagreOgLagJournalførTask(entitet, forespørselEntitet);
        var lukketForespørsel = forespørselBehandlingTjeneste.ferdigstillForespørsel(mottattInntektsmeldingDto.foresporselUuid(), aktorId, orgnummer,
            mottattInntektsmeldingDto.startdato(), LukkeÅrsak.ORDINÆR_INNSENDING);

        var imEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);

        // Metrikker i prometheus
        MetrikkerTjeneste.loggForespørselLukkIntern(lukketForespørsel);
        MetrikkerTjeneste.loggInnsendtInntektsmelding(imEntitet);

        return InntektsmeldingMapper.mapFraEntitet(imEntitet, mottattInntektsmeldingDto.foresporselUuid());
    }

    public InntektsmeldingResponseDto mottaArbeidsgiverInitiertInntektsmelding(SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        var imEnitet = InntektsmeldingMapper.mapTilEntitet(sendInntektsmeldingRequestDto);
        var aktørId = new AktørIdEntitet(sendInntektsmeldingRequestDto.aktorId().id());
        var ytelseType = KodeverkMapper.mapYtelsetype(sendInntektsmeldingRequestDto.ytelse());
        var arbeidsgiverinitiertÅrsak = KodeverkMapper.mapArbeidsgiverinitiertÅrsak(sendInntektsmeldingRequestDto.arbeidsgiverinitiertÅrsakDto());
        var organisasjonsnummer = new OrganisasjonsnummerDto(sendInntektsmeldingRequestDto.arbeidsgiverIdent().ident());

        var forespørselUuid = forespørselBehandlingTjeneste.opprettForespørselForArbeidsgiverInitiertIm(ytelseType,
            aktørId,
            organisasjonsnummer,
            sendInntektsmeldingRequestDto.startdato(),
            arbeidsgiverinitiertÅrsak);

        var forespørselEnitet = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Mangler forespørsel entitet"));

        var imId = lagreOgLagJournalførTask(imEnitet, forespørselEnitet);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid, aktørId, organisasjonsnummer,
            sendInntektsmeldingRequestDto.startdato(), LukkeÅrsak.ORDINÆR_INNSENDING);

        var imEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);

        // Metrikker i prometheus
        MetrikkerTjeneste.logginnsendtArbeidsgiverinitiertIm(imEntitet);

        return InntektsmeldingMapper.mapFraEntitet(imEntitet, forespørselUuid);
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
        task.setProperty(SendTilJoarkTask.KEY_FORESØRSEL_TYPE, forespørsel.getForespørselType().toString());
        prosessTaskTjeneste.lagre(task);
        LOG.info("Opprettet task for oversending til joark");
    }
}
