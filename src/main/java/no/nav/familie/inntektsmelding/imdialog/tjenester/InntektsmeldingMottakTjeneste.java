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
        var imEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);
        var orgnummer = new OrganisasjonsnummerDto(mottattInntektsmeldingDto.arbeidsgiverIdent().ident());
        var aktorId = new AktørIdEntitet(mottattInntektsmeldingDto.aktorId().id());
        var lukketForespørsel = forespørselBehandlingTjeneste.ferdigstillForespørsel(mottattInntektsmeldingDto.foresporselUuid(), aktorId, orgnummer,
            mottattInntektsmeldingDto.startdato(), LukkeÅrsak.ORDINÆR_INNSENDING, imEntitet.getUuid());


        // Metrikker i prometheus
        MetrikkerTjeneste.loggForespørselLukkIntern(lukketForespørsel);
        MetrikkerTjeneste.loggInnsendtInntektsmelding(imEntitet);

        return InntektsmeldingMapper.mapFraEntitet(imEntitet, forespørselEntitet);
    }

    public InntektsmeldingResponseDto mottaArbeidsgiverInitiertNyansattInntektsmelding(SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        var imEnitet = InntektsmeldingMapper.mapTilEntitetArbeidsgiverinitiert(sendInntektsmeldingRequestDto);
        var aktørId = new AktørIdEntitet(sendInntektsmeldingRequestDto.aktorId().id());
        var ytelseType = KodeverkMapper.mapYtelsetype(sendInntektsmeldingRequestDto.ytelse());
        var arbeidsgiverinitiertÅrsak = KodeverkMapper.mapArbeidsgiverinitiertÅrsak(sendInntektsmeldingRequestDto.arbeidsgiverinitiertÅrsak());
        var organisasjonsnummer = new OrganisasjonsnummerDto(sendInntektsmeldingRequestDto.arbeidsgiverIdent().ident());
        var finnesForespørselFraFør = sendInntektsmeldingRequestDto.foresporselUuid() != null;
        if (finnesForespørselFraFør) {
            // endring av allerede innsendt inntektsmelding
            var forespørselEnitet = forespørselBehandlingTjeneste.hentForespørsel(sendInntektsmeldingRequestDto.foresporselUuid())
                .orElseThrow(this::manglerForespørselFeil);
            //hvis ny startdato må også forespørsel oppdateres
            if (sendInntektsmeldingRequestDto.startdato() != forespørselEnitet.getFørsteUttaksdato()) {
                forespørselEnitet = forespørselBehandlingTjeneste.setFørsteUttaksdato(forespørselEnitet, sendInntektsmeldingRequestDto.startdato());
            }

            var imId = lagreOgLagJournalførTask(imEnitet, forespørselEnitet);
            var imEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);

            // Metrikker i prometheus
            MetrikkerTjeneste.loggEndretArbeidsgiverinitiertNyansattIm(imEntitet);

            return InntektsmeldingMapper.mapFraEntitet(imEntitet, forespørselEnitet);
        } else {
            // Inntektsmelding er ny, ikke endring. Må da opprette og ferdigstille forespørsel slik at denne skal finnes i oversikten på Min side - Arbeidsgiver
            // TODO TFP-6425 Både opprettelse og ferdigstillelse her må synkes med dialogporten
            var forespørselEnitet = oppretterArbeidsgiverinitiertForespørsel(ytelseType,
                aktørId,
                organisasjonsnummer,
                arbeidsgiverinitiertÅrsak,
                sendInntektsmeldingRequestDto.startdato());

            var imId = lagreOgLagJournalførTask(imEnitet, forespørselEnitet);
            var imEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);

            forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselEnitet.getUuid(), aktørId, organisasjonsnummer,
                sendInntektsmeldingRequestDto.startdato(), LukkeÅrsak.ORDINÆR_INNSENDING, imEntitet.getUuid());


            // Metrikker i prometheus
            MetrikkerTjeneste.logginnsendtArbeidsgiverinitiertNyansattIm(imEntitet);

            return InntektsmeldingMapper.mapFraEntitet(imEntitet, forespørselEnitet);
        }
    }

    public InntektsmeldingResponseDto mottaArbeidsgiverinitiertUregistrertInntektsmelding(SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        var imEnitet = InntektsmeldingMapper.mapTilEntitet(sendInntektsmeldingRequestDto);

        var aktørId = new AktørIdEntitet(sendInntektsmeldingRequestDto.aktorId().id());
        var ytelseType = KodeverkMapper.mapYtelsetype(sendInntektsmeldingRequestDto.ytelse());
        var arbeidsgiverinitiertÅrsak = KodeverkMapper.mapArbeidsgiverinitiertÅrsak(sendInntektsmeldingRequestDto.arbeidsgiverinitiertÅrsak());
        var organisasjonsnummer = new OrganisasjonsnummerDto(sendInntektsmeldingRequestDto.arbeidsgiverIdent().ident());
        var finnesForespørselFraFør = sendInntektsmeldingRequestDto.foresporselUuid() != null;

        if (finnesForespørselFraFør) {
            // endring av allerede innsendt inntektsmelding
            var forespørselEnitet = forespørselBehandlingTjeneste.hentForespørsel(sendInntektsmeldingRequestDto.foresporselUuid())
                .orElseThrow(this::manglerForespørselFeil);

            var imId = lagreOgLagJournalførTask(imEnitet, forespørselEnitet);
            var imEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);

            // Metrikker i prometheus
            MetrikkerTjeneste.loggEndretArbeidsgiverinitiertUregistrertIm(imEntitet);

            return InntektsmeldingMapper.mapFraEntitet(imEntitet, forespørselEnitet);
        } else {
            // Inntektsmelding er ny, ikke endring. Må da opprette og ferdigstille forespørsel slik at denne skal finnes i oversikten på Min side - Arbeidsgiver
            // TODO TFP-6425 Både opprettelse og ferdigstillelse her må synkes med dialogporten
            var forespørselEnitet = oppretterArbeidsgiverinitiertForespørsel(ytelseType,
                aktørId,
                organisasjonsnummer,
                arbeidsgiverinitiertÅrsak,
                sendInntektsmeldingRequestDto.startdato());

            var imId = lagreOgLagJournalførTask(imEnitet, forespørselEnitet);
            var imEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);

            forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselEnitet.getUuid(), aktørId, organisasjonsnummer,
                sendInntektsmeldingRequestDto.startdato(), LukkeÅrsak.ORDINÆR_INNSENDING, imEntitet.getUuid());


            // Metrikker i prometheus
            MetrikkerTjeneste.logginnsendtArbeidsgiverinitiertUregistrertIm(imEntitet);

            return InntektsmeldingMapper.mapFraEntitet(imEntitet, forespørselEnitet);
        }
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
