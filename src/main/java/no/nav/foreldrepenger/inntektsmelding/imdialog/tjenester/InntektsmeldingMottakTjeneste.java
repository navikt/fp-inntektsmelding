package no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.FellesMottakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ArbeidsgiverinitiertÅrsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class InntektsmeldingMottakTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingMottakTjeneste.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private FellesMottakTjeneste fellesMottakTjeneste;
    private FpsakTjeneste fpsakTjeneste;

    InntektsmeldingMottakTjeneste() {
    }

    @Inject
    public InntektsmeldingMottakTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                         FellesMottakTjeneste fellesMottakTjeneste,
                                         FpsakTjeneste fpsakTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.fellesMottakTjeneste = fellesMottakTjeneste;
        this.fpsakTjeneste = fpsakTjeneste;
    }

    public InntektsmeldingResponseDto mottaInntektsmelding(InntektsmeldingDto mottattInntektsmeldingDto, UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(this::manglerForespørselFeil);

        if (ForespørselStatus.UTGÅTT.equals(forespørsel.status())) {
            throw new IllegalStateException("Kan ikke motta nye inntektsmeldinger på utgåtte forespørsler");
        }

        var lagretIm = fellesMottakTjeneste.lagreOgJournalførInntektsmelding(mottattInntektsmeldingDto, forespørsel);

        MetrikkerTjeneste.loggInnsendtInntektsmelding(lagretIm);

        return InntektsmeldingMapper.mapFraDomene(lagretIm, forespørsel);
    }

    public InntektsmeldingResponseDto mottaArbeidsgiverinitiertInntektsmelding(InntektsmeldingDto inntektsmeldingDto,
                                                                               UUID forespørselUuid,
                                                                               ArbeidsgiverinitiertÅrsak agInitiertÅrsak) {
        var aktørId = inntektsmeldingDto.getAktørId();
        var ytelseType = inntektsmeldingDto.getYtelse();
        var arbeidsgiver = inntektsmeldingDto.getArbeidsgiver();
        var finnesForespørselFraFør = forespørselUuid != null;

        ForespørselDto forespørselDto;
        InntektsmeldingDto lagretInntektsmelding;

        if (finnesForespørselFraFør) {
            forespørselDto = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
                .orElseThrow(this::manglerForespørselFeil);

            if (agInitiertÅrsak == ArbeidsgiverinitiertÅrsak.NYANSATT &&
                inntektsmeldingDto.getStartdato() != forespørselDto.førsteUttaksdato()) {
                forespørselDto = forespørselBehandlingTjeneste.oppdaterFørsteUttaksdato(forespørselDto,
                    inntektsmeldingDto.getStartdato());
            }

            lagretInntektsmelding = fellesMottakTjeneste.lagreOgJournalførInntektsmelding(inntektsmeldingDto, forespørselDto);
            //legger inn oppdatert inntektsmelding i portaler
            forespørselBehandlingTjeneste.oppdaterPortalerMedEndretInntektsmelding(forespørselDto,
                Optional.ofNullable(lagretInntektsmelding.getInntektsmeldingUuid()),
                arbeidsgiver
            );

        } else {
            forespørselDto = oppretterArbeidsgiverinitiertForespørsel(ytelseType,
                aktørId,
                arbeidsgiver,
                agInitiertÅrsak,
                inntektsmeldingDto.getStartdato());

            lagretInntektsmelding = fellesMottakTjeneste.lagreOgJournalførInntektsmelding(inntektsmeldingDto, forespørselDto);
            forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselDto.uuid(), aktørId, arbeidsgiver,
                inntektsmeldingDto.getStartdato(), LukkeÅrsak.ORDINÆR_INNSENDING, Optional.ofNullable(lagretInntektsmelding.getInntektsmeldingUuid()));
        }

        if (agInitiertÅrsak == ArbeidsgiverinitiertÅrsak.NYANSATT) {
            MetrikkerTjeneste.logginnsendtArbeidsgiverinitiertNyansattIm(lagretInntektsmelding);
        } else {
            MetrikkerTjeneste.logginnsendtArbeidsgiverinitiertUregistrertIm(lagretInntektsmelding);
        }
        return InntektsmeldingMapper.mapFraDomene(lagretInntektsmelding, forespørselDto);
    }

    private ForespørselDto oppretterArbeidsgiverinitiertForespørsel(Ytelsetype ytelseType,
                                                                    AktørId aktørId,
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

    private TekniskException manglerForespørselFeil() {
        return new TekniskException("FPINNTEKTSMELDING_FORESPØRSEL_1", "Mangler forespørsel entitet");
    }
}
