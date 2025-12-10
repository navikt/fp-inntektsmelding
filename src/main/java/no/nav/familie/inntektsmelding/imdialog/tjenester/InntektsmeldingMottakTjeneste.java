package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.time.LocalDate;

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
import no.nav.familie.inntektsmelding.integrasjoner.fpsak.FpsakTjeneste;
import no.nav.familie.inntektsmelding.koder.ArbeidsgiverinitiertÅrsak;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class InntektsmeldingMottakTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingMottakTjeneste.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private FpsakTjeneste fpsakTjeneste;

    InntektsmeldingMottakTjeneste() {
    }

    @Inject
    public InntektsmeldingMottakTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                         InntektsmeldingRepository inntektsmeldingRepository,
                                         ProsessTaskTjeneste prosessTaskTjeneste,
                                         FpsakTjeneste fpsakTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.fpsakTjeneste = fpsakTjeneste;
    }

    public InntektsmeldingResponseDto mottaInntektsmelding(SendInntektsmeldingRequestDto mottattInntektsmeldingDto) {
        var forespørselEntitet = forespørselBehandlingTjeneste.hentForespørsel(mottattInntektsmeldingDto.foresporselUuid())
            .orElseThrow(this::manglerForespørselFeil);

        if (ForespørselStatus.UTGÅTT.equals(forespørselEntitet.getStatus())) {
            throw new IllegalStateException("Kan ikke motta nye inntektsmeldinger på utgåtte forespørsler");
        }

        var entitet = InntektsmeldingMapper.mapTilEntitet(mottattInntektsmeldingDto);
        var imId = lagreOgLagJournalførTask(entitet, forespørselEntitet);
        var lagretIm = inntektsmeldingRepository.hentInntektsmelding(imId);
        var orgnummer = new OrganisasjonsnummerDto(mottattInntektsmeldingDto.arbeidsgiverIdent().ident());
        //Ferdigstiller forespørsel hvis den ikke er ferdig fra før
        if (!forespørselEntitet.getStatus().equals(ForespørselStatus.FERDIG)) {
            var aktørId = new AktørIdEntitet(mottattInntektsmeldingDto.aktorId().id());
            var ferdigstiltForespørsel = forespørselBehandlingTjeneste.ferdigstillForespørsel(mottattInntektsmeldingDto.foresporselUuid(), aktørId, orgnummer,
                mottattInntektsmeldingDto.startdato(), LukkeÅrsak.ORDINÆR_INNSENDING, lagretIm.getUuid());
            MetrikkerTjeneste.loggForespørselLukkIntern(ferdigstiltForespørsel);
        } else {
            //legger inn oppdatert inntektsmelding i portaler
            forespørselBehandlingTjeneste.oppdaterPortalerMedEndretInntektsmelding(forespørselEntitet, lagretIm.getUuid(), orgnummer);
        }

        MetrikkerTjeneste.loggInnsendtInntektsmelding(lagretIm);

        return InntektsmeldingMapper.mapFraEntitet(lagretIm, forespørselEntitet);
    }

    public InntektsmeldingResponseDto mottaArbeidsgiverinitiertInntektsmelding(
        SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto,
        ArbeidsgiverinitiertÅrsak årsak) {
        var nyInntektsmelding = (årsak == ArbeidsgiverinitiertÅrsak.NYANSATT)
                                ? InntektsmeldingMapper.mapTilEntitetArbeidsgiverinitiert(sendInntektsmeldingRequestDto)
                                : InntektsmeldingMapper.mapTilEntitet(sendInntektsmeldingRequestDto);
        var aktørId = new AktørIdEntitet(sendInntektsmeldingRequestDto.aktorId().id());
        var ytelseType = KodeverkMapper.mapYtelsetype(sendInntektsmeldingRequestDto.ytelse());
        var arbeidsgiverinitiertÅrsak = KodeverkMapper.mapArbeidsgiverinitiertÅrsak(sendInntektsmeldingRequestDto.arbeidsgiverinitiertÅrsak());
        var organisasjonsnummer = new OrganisasjonsnummerDto(sendInntektsmeldingRequestDto.arbeidsgiverIdent().ident());
        var finnesForespørselFraFør = sendInntektsmeldingRequestDto.foresporselUuid() != null;
        ForespørselEntitet forespørselEnitet;
        InntektsmeldingEntitet lagretInntektsmelding;

        if (finnesForespørselFraFør) {
            forespørselEnitet = forespørselBehandlingTjeneste.hentForespørsel(sendInntektsmeldingRequestDto.foresporselUuid())
                .orElseThrow(this::manglerForespørselFeil);

            if (årsak == ArbeidsgiverinitiertÅrsak.NYANSATT &&
                sendInntektsmeldingRequestDto.startdato() != forespørselEnitet.getFørsteUttaksdato()) {
                forespørselEnitet = forespørselBehandlingTjeneste.oppdaterFørsteUttaksdato(forespørselEnitet,
                    sendInntektsmeldingRequestDto.startdato());
            }

            lagretInntektsmelding = lagreOgJournalførInntektsmelding(nyInntektsmelding, forespørselEnitet);
            //legger inn oppdatert inntektsmelding i portaler
            forespørselBehandlingTjeneste.oppdaterPortalerMedEndretInntektsmelding(forespørselEnitet,
                lagretInntektsmelding.getUuid(),
                organisasjonsnummer
            );

        } else {
            forespørselEnitet = oppretterArbeidsgiverinitiertForespørsel(ytelseType,
                aktørId,
                organisasjonsnummer,
                arbeidsgiverinitiertÅrsak,
                sendInntektsmeldingRequestDto.startdato());

            lagretInntektsmelding = lagreOgJournalførInntektsmelding(nyInntektsmelding, forespørselEnitet);
            forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselEnitet.getUuid(), aktørId, organisasjonsnummer,
                sendInntektsmeldingRequestDto.startdato(), LukkeÅrsak.ORDINÆR_INNSENDING, lagretInntektsmelding.getUuid());
        }

        if (årsak == ArbeidsgiverinitiertÅrsak.NYANSATT) {
            MetrikkerTjeneste.logginnsendtArbeidsgiverinitiertNyansattIm(lagretInntektsmelding);
        } else {
            MetrikkerTjeneste.logginnsendtArbeidsgiverinitiertUregistrertIm(lagretInntektsmelding);
        }
        return InntektsmeldingMapper.mapFraEntitet(lagretInntektsmelding, forespørselEnitet);
    }

    private InntektsmeldingEntitet lagreOgJournalførInntektsmelding(InntektsmeldingEntitet imEnitet, ForespørselEntitet forespørselEnitet) {
        var imId = lagreOgLagJournalførTask(imEnitet, forespørselEnitet);
        return inntektsmeldingRepository.hentInntektsmelding(imId);
    }

    private ForespørselEntitet oppretterArbeidsgiverinitiertForespørsel(Ytelsetype ytelseType, AktørIdEntitet aktørId,
                                                                        OrganisasjonsnummerDto organisasjonsnummer,
                                                                        ArbeidsgiverinitiertÅrsak arbeidsgiverinitiertÅrsak,
                                                                        LocalDate startdato) {
        // dersom uregistrert så må vi hente skjæringstidspunkt fra fpsak. Vi trenger denne for å hente riktig inntektsperioder ved endring av inntektsmelding
        LocalDate skjæringstidspunkt = Tid.TIDENES_ENDE;
        if (arbeidsgiverinitiertÅrsak.equals(ArbeidsgiverinitiertÅrsak.UREGISTRERT)) {
            skjæringstidspunkt = fpsakTjeneste.henterInfoOmSakIFagsystem(aktørId, ytelseType).skjæringstidspunkt();
        }

        var forespørselUuid = forespørselBehandlingTjeneste.opprettForespørselForArbeidsgiverInitiertIm(ytelseType,
            aktørId,
            organisasjonsnummer,
            startdato,
            arbeidsgiverinitiertÅrsak,
            skjæringstidspunkt == Tid.TIDENES_ENDE ? null : skjæringstidspunkt);

        return forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(this::manglerForespørselFeil);
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

    private TekniskException manglerForespørselFeil() {
        return new TekniskException("FPINNTEKTSMELDING_FORESPØRSEL_1", "Mangler forespørsel entitet");
    }
}
