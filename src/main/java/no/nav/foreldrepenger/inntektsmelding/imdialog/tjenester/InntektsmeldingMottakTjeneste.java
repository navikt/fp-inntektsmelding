package no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.task.SendTilJoarkTask;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ArbeidsgiverinitiertÅrsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
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
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(mottattInntektsmeldingDto.foresporselUuid())
            .orElseThrow(this::manglerForespørselFeil);

        if (ForespørselStatus.UTGÅTT.equals(forespørsel.status())) {
            throw new IllegalStateException("Kan ikke motta nye inntektsmeldinger på utgåtte forespørsler");
        }

        var inntektsmeldingDto = InntektsmeldingMapper.mapTilDto(mottattInntektsmeldingDto);

        var lagretIm = lagreOgJournalførInntektsmelding(inntektsmeldingDto, forespørsel);
        //Ferdigstiller forespørsel hvis den ikke er ferdig fra før

        var arbeidsgiver = Arbeidsgiver.fra(mottattInntektsmeldingDto.arbeidsgiverIdent().orgnr());
        if (!forespørsel.status().equals(ForespørselStatus.FERDIG)) {
            var aktørId = AktørId.fra(mottattInntektsmeldingDto.aktorId().id());
            var ferdigstiltForespørsel = forespørselBehandlingTjeneste.ferdigstillForespørsel(mottattInntektsmeldingDto.foresporselUuid(), aktørId,
                arbeidsgiver,
                mottattInntektsmeldingDto.startdato(), LukkeÅrsak.ORDINÆR_INNSENDING, Optional.ofNullable(lagretIm.getInntektsmeldingUuid()));
            MetrikkerTjeneste.loggForespørselLukkIntern(ferdigstiltForespørsel);
        } else {
            //legger inn oppdatert inntektsmelding i portaler
            forespørselBehandlingTjeneste.oppdaterPortalerMedEndretInntektsmelding(forespørsel, Optional.ofNullable(lagretIm.getInntektsmeldingUuid()),
                arbeidsgiver);
        }

        MetrikkerTjeneste.loggInnsendtInntektsmelding(lagretIm);

        return InntektsmeldingMapper.mapFraDomene(lagretIm, forespørsel);
    }

    public InntektsmeldingResponseDto mottaArbeidsgiverinitiertInntektsmelding(SendInntektsmeldingRequestDto requestDto, ArbeidsgiverinitiertÅrsak årsak) {
        var nyInntektsmelding = (årsak == ArbeidsgiverinitiertÅrsak.NYANSATT)
                                ? InntektsmeldingMapper.mapTilDtoArbeidsgiverinitiert(requestDto)
                                : InntektsmeldingMapper.mapTilDto(requestDto);
        var aktørId = AktørId.fra(requestDto.aktorId().id());
        var ytelseType = KodeverkMapper.mapYtelsetype(requestDto.ytelse());
        var arbeidsgiverinitiertÅrsak = KodeverkMapper.mapArbeidsgiverinitiertÅrsak(requestDto.arbeidsgiverinitiertÅrsak());
        var arbeidsgiver = Arbeidsgiver.fra(requestDto.arbeidsgiverIdent().orgnr());
        var finnesForespørselFraFør = requestDto.foresporselUuid() != null;
        ForespørselDto forespørselDto;
        InntektsmeldingDto lagretInntektsmelding;

        if (finnesForespørselFraFør) {
            forespørselDto = forespørselBehandlingTjeneste.hentForespørsel(requestDto.foresporselUuid())
                .orElseThrow(this::manglerForespørselFeil);

            if (årsak == ArbeidsgiverinitiertÅrsak.NYANSATT &&
                requestDto.startdato() != forespørselDto.førsteUttaksdato()) {
                forespørselDto = forespørselBehandlingTjeneste.oppdaterFørsteUttaksdato(forespørselDto,
                    requestDto.startdato());
            }

            lagretInntektsmelding = lagreOgJournalførInntektsmelding(nyInntektsmelding, forespørselDto);
            //legger inn oppdatert inntektsmelding i portaler
            forespørselBehandlingTjeneste.oppdaterPortalerMedEndretInntektsmelding(forespørselDto,
                Optional.ofNullable(lagretInntektsmelding.getInntektsmeldingUuid()),
                arbeidsgiver
            );

        } else {
            forespørselDto = oppretterArbeidsgiverinitiertForespørsel(ytelseType,
                aktørId,
                arbeidsgiver,
                arbeidsgiverinitiertÅrsak,
                requestDto.startdato());

            lagretInntektsmelding = lagreOgJournalførInntektsmelding(nyInntektsmelding, forespørselDto);
            forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselDto.uuid(), aktørId, arbeidsgiver,
                requestDto.startdato(), LukkeÅrsak.ORDINÆR_INNSENDING, Optional.ofNullable(lagretInntektsmelding.getInntektsmeldingUuid()));
        }

        if (årsak == ArbeidsgiverinitiertÅrsak.NYANSATT) {
            MetrikkerTjeneste.logginnsendtArbeidsgiverinitiertNyansattIm(lagretInntektsmelding);
        } else {
            MetrikkerTjeneste.logginnsendtArbeidsgiverinitiertUregistrertIm(lagretInntektsmelding);
        }
        return InntektsmeldingMapper.mapFraDomene(lagretInntektsmelding, forespørselDto);
    }

    private InntektsmeldingDto lagreOgJournalførInntektsmelding(InntektsmeldingDto inntektsmeldingDto, ForespørselDto forespørsel) {
        var imId = lagreOgLagJournalførTask(inntektsmeldingDto, forespørsel);
        return inntektsmeldingTjeneste.hentInntektsmelding(imId);
    }

    private ForespørselDto oppretterArbeidsgiverinitiertForespørsel(Ytelsetype ytelseType, AktørId aktørId,
                                                                    Arbeidsgiver arbeidsgiver,
                                                                    ArbeidsgiverinitiertÅrsak arbeidsgiverinitiertÅrsak,
                                                                    LocalDate startdato) {
        // dersom uregistrert så må vi hente skjæringstidspunkt fra fpsak. Vi trenger denne for å hente riktig inntektsperioder ved endring av inntektsmelding
        LocalDate skjæringstidspunkt = Tid.TIDENES_ENDE;
        if (arbeidsgiverinitiertÅrsak.equals(ArbeidsgiverinitiertÅrsak.UREGISTRERT)) {
            skjæringstidspunkt = fpsakTjeneste.henterInfoOmSakIFagsystem(aktørId, ytelseType).skjæringstidspunkt();
        }

        var forespørselUuid = forespørselBehandlingTjeneste.opprettForespørselForArbeidsgiverInitiertIm(ytelseType,
            aktørId,
            arbeidsgiver,
            startdato,
            arbeidsgiverinitiertÅrsak,
            skjæringstidspunkt == Tid.TIDENES_ENDE ? null : skjæringstidspunkt);

        return forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(this::manglerForespørselFeil);
    }

    private Long lagreOgLagJournalførTask(InntektsmeldingDto inntektsmeldingDto, ForespørselDto forespørsel) {
        LOG.info("Lagrer inntektsmelding for forespørsel {}", forespørsel.uuid());
        var imId = inntektsmeldingTjeneste.lagreInntektsmelding(inntektsmeldingDto);
        opprettTaskForSendTilJoark(imId, forespørsel);
        return imId;
    }

    private void opprettTaskForSendTilJoark(Long imId, ForespørselDto forespørsel) {
        var task = ProsessTaskData.forProsessTask(SendTilJoarkTask.class);
        var saksnummer = forespørsel.fagsystemSaksnummer();
        if (saksnummer != null) {
            task.setSaksnummer(saksnummer.saksnummer());
        }
        task.setProperty(SendTilJoarkTask.KEY_INNTEKTSMELDING_ID, imId.toString());
        task.setProperty(SendTilJoarkTask.KEY_FORESPOERSEL_TYPE, forespørsel.forespørselType().name());
        prosessTaskTjeneste.lagre(task);
        LOG.info("Opprettet task for oversending til joark");
    }

    private TekniskException manglerForespørselFeil() {
        return new TekniskException("FPINNTEKTSMELDING_FORESPØRSEL_1", "Mangler forespørsel entitet");
    }
}
