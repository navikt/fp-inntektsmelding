package no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselValiderer;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.FellesMottakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakFagsak;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ArbeidsgiverinitiertÅrsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class InntektsmeldingMottakTjeneste {
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

        //Validering
        if (ForespørselStatus.UTGÅTT.equals(forespørsel.status())) {
            throw new IllegalStateException("Kan ikke motta nye inntektsmeldinger på utgåtte forespørsler");
        }
        ForespørselValiderer.validerAktør(forespørsel, mottattInntektsmeldingDto.getAktørId());
        ForespørselValiderer.validerOrganisasjon(forespørsel, mottattInntektsmeldingDto.getArbeidsgiver());
        ForespørselValiderer.validerStartdato(forespørsel, mottattInntektsmeldingDto.getStartdato());

        var lagretIm = fellesMottakTjeneste.lagreImOgOpprettJournalførTask(mottattInntektsmeldingDto, forespørsel);
        fellesMottakTjeneste.ferdigstillOgOppdaterEksterneSystemer(forespørsel, Optional.ofNullable(lagretIm.getInntektsmeldingUuid()));

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
            //Validering
            ForespørselValiderer.validerAktør(forespørselDto, inntektsmeldingDto.getAktørId());
            ForespørselValiderer.validerOrganisasjon(forespørselDto, inntektsmeldingDto.getArbeidsgiver());

            if (agInitiertÅrsak == ArbeidsgiverinitiertÅrsak.NYANSATT &&
                !inntektsmeldingDto.getStartdato().equals(forespørselDto.førsteUttaksdato())) {
                // Ved arbeidsgiverinitiert innsending for nyansatt er det tillatt å endre startdato,
                // derfor valideres ikke startdato mot forespørselen før den er oppdatert med den nye datoen
                forespørselDto = forespørselBehandlingTjeneste.oppdaterFørsteUttaksdato(forespørselDto,
                    inntektsmeldingDto.getStartdato());
            } else {
                ForespørselValiderer.validerStartdato(forespørselDto, inntektsmeldingDto.getStartdato());
            }

            lagretInntektsmelding = fellesMottakTjeneste.lagreImOgOpprettJournalførTask(inntektsmeldingDto, forespørselDto);
            //legger inn oppdatert inntektsmelding i portaler
            forespørselBehandlingTjeneste.opprettTasksForÅOppdaterePortaler(forespørselDto,
                Optional.ofNullable(lagretInntektsmelding.getInntektsmeldingUuid())
            );

        } else {
            var muligeRelevanteFagsaker = fpsakTjeneste.henterInfoOmSakIFagsystem(aktørId, ytelseType).stream()
                .filter(a -> a.statusInntektsmelding().equals(FpsakFagsak.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING))
                .toList();
            var saksnummer = muligeRelevanteFagsaker.size() == 1 ? muligeRelevanteFagsaker.getFirst().saksnummer() : null;
            // dersom uregistrert så må vi hente skjæringstidspunkt fra fpsak. Vi trenger denne for å hente riktig inntektsperioder ved endring av inntektsmelding
            LocalDate skjæringstidspunkt = Tid.TIDENES_ENDE;
            if (agInitiertÅrsak.equals(ArbeidsgiverinitiertÅrsak.UREGISTRERT)) {
                var infoOmSak = fpsakTjeneste.henterInfoOmSakIFagsystem(aktørId, ytelseType).stream()
                    .filter(s -> FpsakFagsak.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING.equals(s.statusInntektsmelding()))
                    .min(Comparator.comparing(FpsakFagsak::førsteUttaksdato))
                    .orElseThrow(() -> new IllegalStateException("Mangler sak i fpsak"));
                skjæringstidspunkt = infoOmSak.skjæringstidspunkt();
            }
            //oppretter forespørsel i databasen
            forespørselDto = forespørselBehandlingTjeneste.opprettForespørselForArbeidsgiverInitiertIm(ytelseType,
                aktørId,
                arbeidsgiver,
                inntektsmeldingDto.getStartdato(),
                agInitiertÅrsak,
                Tid.TIDENES_ENDE.equals(skjæringstidspunkt) ? null : skjæringstidspunkt,
                saksnummer);

            lagretInntektsmelding = fellesMottakTjeneste.lagreImOgOpprettJournalførTask(inntektsmeldingDto, forespørselDto);

            forespørselBehandlingTjeneste.opprettTasksForOpprettOgFerdigstillAgi(forespørselDto,
                lagretInntektsmelding.getInntektsmeldingUuid());
        }

        if (agInitiertÅrsak == ArbeidsgiverinitiertÅrsak.NYANSATT) {
            MetrikkerTjeneste.logginnsendtArbeidsgiverinitiertNyansattIm(lagretInntektsmelding);
        } else {
            MetrikkerTjeneste.logginnsendtArbeidsgiverinitiertUregistrertIm(lagretInntektsmelding);
        }
        return InntektsmeldingMapper.mapFraDomene(lagretInntektsmelding, forespørselDto);
    }

    private TekniskException manglerForespørselFeil() {
        return new TekniskException("FPINNTEKTSMELDING_FORESPØRSEL_1", "Mangler forespørsel entitet");
    }
}
