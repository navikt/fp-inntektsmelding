package no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.task.SendTilJoarkTask;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ArbeidsgiverinitiertÅrsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørId;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class InntektsmeldingMottakTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingMottakTjeneste.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private FpsakTjeneste fpsakTjeneste;

    InntektsmeldingMottakTjeneste() {
    }

    @Inject
    public InntektsmeldingMottakTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                         InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                         ProsessTaskTjeneste prosessTaskTjeneste,
                                         FpsakTjeneste fpsakTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.fpsakTjeneste = fpsakTjeneste;
    }

    public InntektsmeldingResponseDto mottaInntektsmelding(SendInntektsmeldingRequestDto mottattInntektsmeldingDto) {
        var forespørselEntitet = forespørselBehandlingTjeneste.hentForespørsel(mottattInntektsmeldingDto.foresporselUuid())
            .orElseThrow(this::manglerForespørselFeil);

        if (ForespørselStatus.UTGÅTT.equals(forespørselEntitet.getStatus())) {
            throw new IllegalStateException("Kan ikke motta nye inntektsmeldinger på utgåtte forespørsler");
        }

        var inntektsmeldingDto = InntektsmeldingMapper.mapTilDto(mottattInntektsmeldingDto);

        var lagretIm = lagreOgJournalførInntektsmelding(inntektsmeldingDto, forespørselEntitet);
        var orgnummer = new OrganisasjonsnummerDto(mottattInntektsmeldingDto.arbeidsgiverIdent().orgnr());
        //Ferdigstiller forespørsel hvis den ikke er ferdig fra før
        if (!forespørselEntitet.getStatus().equals(ForespørselStatus.FERDIG)) {
            var aktørId = new AktørId(mottattInntektsmeldingDto.aktorId().id());
            var ferdigstiltForespørsel = forespørselBehandlingTjeneste.ferdigstillForespørsel(mottattInntektsmeldingDto.foresporselUuid(), aktørId, orgnummer,
                mottattInntektsmeldingDto.startdato(), LukkeÅrsak.ORDINÆR_INNSENDING, Optional.ofNullable(lagretIm.getInntektsmeldingUuid()));
            MetrikkerTjeneste.loggForespørselLukkIntern(ferdigstiltForespørsel);
        } else {
            //legger inn oppdatert inntektsmelding i portaler
            forespørselBehandlingTjeneste.oppdaterPortalerMedEndretInntektsmelding(forespørselEntitet, Optional.ofNullable(lagretIm.getInntektsmeldingUuid()), orgnummer);
        }

        MetrikkerTjeneste.loggInnsendtInntektsmelding(lagretIm);

        return InntektsmeldingMapper.mapFraDomene(lagretIm, forespørselEntitet);
    }

    public InntektsmeldingResponseDto mottaArbeidsgiverinitiertInntektsmelding(
        SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto,
        ArbeidsgiverinitiertÅrsak årsak) {
        var nyInntektsmelding = (årsak == ArbeidsgiverinitiertÅrsak.NYANSATT)
                                ? InntektsmeldingMapper.mapTilDtoArbeidsgiverinitiert(sendInntektsmeldingRequestDto)
                                : InntektsmeldingMapper.mapTilDto(sendInntektsmeldingRequestDto);
        var aktørId = new AktørId(sendInntektsmeldingRequestDto.aktorId().id());
        var ytelseType = KodeverkMapper.mapYtelsetype(sendInntektsmeldingRequestDto.ytelse());
        var arbeidsgiverinitiertÅrsak = KodeverkMapper.mapArbeidsgiverinitiertÅrsak(sendInntektsmeldingRequestDto.arbeidsgiverinitiertÅrsak());
        var organisasjonsnummer = new OrganisasjonsnummerDto(sendInntektsmeldingRequestDto.arbeidsgiverIdent().orgnr());
        var finnesForespørselFraFør = sendInntektsmeldingRequestDto.foresporselUuid() != null;
        ForespørselEntitet forespørselEnitet;
        InntektsmeldingDto lagretInntektsmelding;

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
                Optional.ofNullable(lagretInntektsmelding.getInntektsmeldingUuid()),
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
                sendInntektsmeldingRequestDto.startdato(), LukkeÅrsak.ORDINÆR_INNSENDING, Optional.ofNullable(lagretInntektsmelding.getInntektsmeldingUuid()));
        }

        if (årsak == ArbeidsgiverinitiertÅrsak.NYANSATT) {
            MetrikkerTjeneste.logginnsendtArbeidsgiverinitiertNyansattIm(lagretInntektsmelding);
        } else {
            MetrikkerTjeneste.logginnsendtArbeidsgiverinitiertUregistrertIm(lagretInntektsmelding);
        }
        return InntektsmeldingMapper.mapFraDomene(lagretInntektsmelding, forespørselEnitet);
    }

    private InntektsmeldingDto lagreOgJournalførInntektsmelding(InntektsmeldingDto inntektsmeldingDto, ForespørselEntitet forespørselEnitet) {
        var imId = lagreOgLagJournalførTask(inntektsmeldingDto, forespørselEnitet);
        return inntektsmeldingTjeneste.hentInntektsmelding(imId);
    }

    private ForespørselEntitet oppretterArbeidsgiverinitiertForespørsel(Ytelsetype ytelseType, AktørId aktørId,
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

    private Long lagreOgLagJournalførTask(InntektsmeldingDto inntektsmeldingDto, ForespørselEntitet forespørsel) {
        LOG.info("Lagrer inntektsmelding for forespørsel {}", forespørsel.getUuid());
        var imId = inntektsmeldingTjeneste.lagreInntektsmelding(inntektsmeldingDto);
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
