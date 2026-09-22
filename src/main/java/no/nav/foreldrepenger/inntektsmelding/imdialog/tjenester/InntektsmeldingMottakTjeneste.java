package no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselValiderer;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.FellesMottakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektKontrollResultat;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektKontrollTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakFagsak;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
import no.nav.foreldrepenger.inntektsmelding.server.exceptions.InntektAvvikerFraAInntektException;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ArbeidsgiverinitiertÅrsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.InntektsmeldingStatus;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class InntektsmeldingMottakTjeneste {
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private FellesMottakTjeneste fellesMottakTjeneste;
    private FpsakTjeneste fpsakTjeneste;
    private InntektKontrollTjeneste inntektKontrollTjeneste;

    InntektsmeldingMottakTjeneste() {
    }

    @Inject
    public InntektsmeldingMottakTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                         FellesMottakTjeneste fellesMottakTjeneste,
                                         FpsakTjeneste fpsakTjeneste,
                                         InntektKontrollTjeneste inntektKontrollTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.fellesMottakTjeneste = fellesMottakTjeneste;
        this.fpsakTjeneste = fpsakTjeneste;
        this.inntektKontrollTjeneste = inntektKontrollTjeneste;
    }

    public InntektsmeldingResponseDto mottaInntektsmelding(InntektsmeldingDto mottattInntektsmeldingDto, UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid);

        //Validering
        if (ForespørselStatus.UTGÅTT.equals(forespørsel.status())) {
            throw new IllegalStateException("Kan ikke motta nye inntektsmeldinger på utgåtte forespørsler");
        }
        ForespørselValiderer.validerAktør(forespørsel, mottattInntektsmeldingDto.getAktørId());
        ForespørselValiderer.validerOrganisasjon(forespørsel, mottattInntektsmeldingDto.getArbeidsgiver());
        ForespørselValiderer.validerStartdato(forespørsel, mottattInntektsmeldingDto.getStartdato());

        fellesMottakTjeneste.settForrigeInntektsmeldingUtdatertHvisVenterVurdering(forespørsel);

        //Vi trenger ikke å sjekke inntekt om årsak allerede er oppgitt
        if (mottattInntektsmeldingDto.getEndringAvInntektÅrsaker().isEmpty()) {
            var kontrollResultat = inntektKontrollTjeneste.sjekkInntektMotAInntekt(forespørsel, mottattInntektsmeldingDto);

            if (kontrollResultat instanceof InntektKontrollResultat.UlikInntekt(_, var inntektFraAInntekt)) {
                throw new InntektAvvikerFraAInntektException(inntektFraAInntekt.gjennomsnitt(), mottattInntektsmeldingDto.getMånedInntekt());
            }

            if (kontrollResultat instanceof InntektKontrollResultat.Nedetid) {
                // A-inntekt har nedetid - lagrer inntektsmeldingen med status VENTER_VURDERING og etterkontrollerer
                // asynkront (se InntektKontrollTjeneste.kontrollerInntektsmeldingEtterNedetid). Forespørselen
                // ferdigstilles ikke, og portalene varsles først når resultatet av etterkontrollen foreligger.
                var inntektsmeldingMedStatus = InntektsmeldingDto.builder(mottattInntektsmeldingDto)
                    .medStatus(InntektsmeldingStatus.VENTER_VURDERING)
                    .build();
                var lagretIm = fellesMottakTjeneste.lagreImOgOpprettTaskForEtterkontroll(inntektsmeldingMedStatus, forespørsel);
                MetrikkerTjeneste.loggInnsendtInntektsmeldingUnderNedetid();
                return InntektsmeldingMapper.mapFraDomene(lagretIm, forespørsel);
            }
        }

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
            forespørselDto = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid);
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

            //opdaterer status til FERDIG - må bruke uuid siden forespørselen ikke er oppdatert med sak id fra arbeidsgiverportalen enda
            forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselDto.uuid());

            lagretInntektsmelding = fellesMottakTjeneste.lagreImOgOpprettJournalførTask(inntektsmeldingDto, forespørselDto);

            forespørselBehandlingTjeneste.opprettSakOgFerdigstillTasksIPortaler(forespørselDto,
                lagretInntektsmelding.getInntektsmeldingUuid());
        }

        if (agInitiertÅrsak == ArbeidsgiverinitiertÅrsak.NYANSATT) {
            MetrikkerTjeneste.logginnsendtArbeidsgiverinitiertNyansattIm(lagretInntektsmelding);
        } else {
            MetrikkerTjeneste.logginnsendtArbeidsgiverinitiertUregistrertIm(lagretInntektsmelding);
        }
        return InntektsmeldingMapper.mapFraDomene(lagretInntektsmelding, forespørselDto);
    }

}
