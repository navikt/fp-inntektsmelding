package no.nav.foreldrepenger.inntektsmelding.imapi.rest.tjenester;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.InntektsmeldingStatus;
import no.nav.vedtak.exception.TekniskException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.felles.FeilkodeDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.FellesGrunnlagTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.FellesMottakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;

@ApplicationScoped
public class InntektsmeldingApiMottakTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingApiMottakTjeneste.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private FellesMottakTjeneste fellesMottakTjeneste;
    private FellesGrunnlagTjeneste fellesGrunnlagTjeneste;
    private InntektTjeneste inntektTjeneste;
    private PersonTjeneste  personTjeneste;
    private static final BigDecimal AKSEPTERT_AVVIK = new BigDecimal("50");

    InntektsmeldingApiMottakTjeneste() {
        //CDI
    }

    @Inject
    public InntektsmeldingApiMottakTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                            InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                            FellesMottakTjeneste fellesMottakTjeneste,
                                            InntektTjeneste inntektTjeneste,
                                            FellesGrunnlagTjeneste fellesGrunnlagTjeneste,
                                            PersonTjeneste personTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.fellesMottakTjeneste = fellesMottakTjeneste;
        this.inntektTjeneste = inntektTjeneste;
        this.fellesGrunnlagTjeneste = fellesGrunnlagTjeneste;
        this.personTjeneste = personTjeneste;
    }

    public SendInntektsmeldingResponse mottaInntektsmelding(InntektsmeldingDto inntektsmelding, UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid).orElse(null);
        if (forespørsel == null) {
            LOG.info("Finner ikke forespørsel for uuid {}", forespørselUuid);
            return new SendInntektsmeldingResponse(false,
                null, null,
                new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.TOM_FORESPOERSEL, "Finner ikke forespørsel for uuid " + forespørselUuid,
                    forespørselUuid.toString()));
        }

        if (ForespørselStatus.UTGÅTT.equals(forespørsel.status())) {
            LOG.info("Forespørsel har status utgått. Inntektsmelding kan ikke mottas. forespørselUuid: {}", forespørselUuid);
            return new SendInntektsmeldingResponse(false,
                null, null,
                new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.UGYLDIG_FORESPOERSEL,
                    "Det er ikke tillatt å sende inn en inntektsmelding på en forkastet forespørsel",
                    forespørselUuid.toString()));
        }

        var sisteInntektsmelding = inntektsmeldingTjeneste.hentSisteInntektsmeldingForForespørsel(forespørsel.uuid());
        if (sisteInntektsmelding != null && inntektsmeldingerErLike(inntektsmelding, sisteInntektsmelding)) {
            LOG.info(
                "Inntektsmelding avvises. Ingen endring på ny inntektsmelding sammenlignet med tidligere innsendt inntektsmelding. inntektsmeldingId: {}",
                inntektsmelding.getId());
            return new SendInntektsmeldingResponse(false, null, null,
                new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.DUPLIKAT,
                    "Inntektsmelding avvises. Ingen endring på ny inntektsmelding sammenlignet med tidligere innsendt inntektsmelding med id: "
                        + sisteInntektsmelding.getInntektsmeldingUuid(),
                    sisteInntektsmelding.getInntektsmeldingUuid().toString()));
        }

        var sendInntektsmeldingResponse = sjekkMånedInntektMotRapportertInntekt(forespørsel, inntektsmelding);
        if (!sendInntektsmeldingResponse.success()) {
            return sendInntektsmeldingResponse;
        }

        var lagretIm = fellesMottakTjeneste.lagreOgJournalførInntektsmelding(inntektsmelding, forespørsel);
        fellesMottakTjeneste.ferdigstillOgOppdaterEksterneSystemer(forespørsel, Optional.ofNullable(lagretIm.getInntektsmeldingUuid()));

        MetrikkerTjeneste.loggInnsendtInntektsmelding(lagretIm);
        return new SendInntektsmeldingResponse(true, lagretIm.getInntektsmeldingUuid(), InntektsmeldingKontraktMapper.mapTilKontrakt(lagretIm.getStatus()), null);
    }

    public void kontrollerInntektsmeldingEtterNedetid(Long inntektsmeldingId) {
        var inntektsmelding = inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId);
        var forespørsel = inntektsmelding.getForespørsel().orElseThrow();
        var personInfo = personTjeneste.hentPersonInfoFraAktørId(inntektsmelding.getAktørId(), inntektsmelding.getYtelse());
        var harJobbetHeleBeregningsperioden = fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(personInfo,
            forespørsel.skjæringstidspunkt(),
            inntektsmelding.getArbeidsgiver());
        var inntekter = inntektTjeneste.hentInntekt(inntektsmelding.getAktørId(),
            forespørsel.skjæringstidspunkt(),
            LocalDate.now(),
            inntektsmelding.getArbeidsgiver(),
            harJobbetHeleBeregningsperioden);

        if (inntekter.harNedetid()) {
            throw new TekniskException("F-523043", "Nedetid i a-inntekt, får ikke ferdigstilt inntektsmelding " + inntektsmeldingId);
        }

        var inntektErUgyldig = erOppgittInntektUgyldig(inntektsmelding, inntekter);

        if (inntektErUgyldig) {
            inntektsmeldingTjeneste.oppdatertStatusTilInntektsmelding(inntektsmelding.getInntektsmeldingUuid(), InntektsmeldingStatus.AVVIST);
            var feilmelding = String.format(
                "Inntekt i inntektsmelding er ulik inntekt fra A-inntekt, og ingen endringsårsak er oppgitt. Gjennomsnittlig inntekt fra A-inntekt: %s, oppgitt inntekt i inntektsmelding: %s",
                inntekter.gjennomsnitt(),
                inntektsmelding.getMånedInntekt());
            forespørselBehandlingTjeneste.sendMeldingOmAvvistInntektsmelding(forespørsel, feilmelding);
        } else {
            inntektsmeldingTjeneste.oppdatertStatusTilInntektsmelding(inntektsmelding.getInntektsmeldingUuid(), InntektsmeldingStatus.GODKJENT);
            fellesMottakTjeneste.opprettTaskForSendTilJoark(inntektsmeldingId, forespørsel);
            fellesMottakTjeneste.ferdigstillOgOppdaterEksterneSystemer(forespørsel, Optional.ofNullable(inntektsmelding.getInntektsmeldingUuid()));
            MetrikkerTjeneste.loggInnsendtInntektsmelding(inntektsmelding);
        }
    }

    private SendInntektsmeldingResponse sjekkMånedInntektMotRapportertInntekt(ForespørselDto forespørsel, InntektsmeldingDto inntektsmelding) {
        var personInfo = personTjeneste.hentPersonInfoFraAktørId(forespørsel.aktørId(), forespørsel.ytelseType());
        var harJobbetHeleBeregningsperioden = fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(personInfo,
            forespørsel.skjæringstidspunkt(),
            forespørsel.arbeidsgiver());
        var inntektFraAInntekt = inntektTjeneste.hentInntekt(forespørsel.aktørId(),
            forespørsel.skjæringstidspunkt(),
            LocalDate.now(),
            forespørsel.arbeidsgiver(),
            harJobbetHeleBeregningsperioden);

        //Dette skal egentlig ikke skje...
        if (inntektFraAInntekt == null) {
            LOG.warn(
                "InntektTjenteste har ikke returnert inntekt, og vi kan ikke verifisere inntekt i inntektsmeldingen mot A-inntekt. inntektsmeldingId: {}",
                inntektsmelding.getId());
            throw new IllegalStateException("InntektsmeldingApiMottakTjeneste: utviklerfeil - får tom inntekt fra A-inntekt");
        }

        if (inntektFraAInntekt.harNedetid()) {
            LOG.warn(
                "Inntektskomponenten har nedetid, og vi kan ikke verifisere inntekt i inntektsmeldingen mot A-inntekt. inntektsmeldingId: {}",
                inntektsmelding.getId());
            // TODO oppdater status i dialogporten
            InntektsmeldingDto inntektsmeldingMedStatus = InntektsmeldingDto.builder(inntektsmelding).medStatus(InntektsmeldingStatus.VENTER_VURDERING).build();

            fellesMottakTjeneste.lagreIMOgOpprettTaskForEtterkontroll(inntektsmeldingMedStatus, forespørsel);
            MetrikkerTjeneste.loggInnsendtInntektsmeldingUnderNedetid();
            return new SendInntektsmeldingResponse(false,
                null,
                null,
                new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.NEDETID_AINNTEKT,
                    "Inntektskomponenten har nedetid, og vi kan ikke verifisere inntekt i inntektsmeldingen mot A-inntekt. Prøv igjen om litt.",
                    forespørsel.uuid().toString()));
        }

        var inntektErUlikOgIngenÅrsakOppgitt = erOppgittInntektUgyldig(inntektsmelding, inntektFraAInntekt);

        if (inntektErUlikOgIngenÅrsakOppgitt) {
            var feilmelding = String.format(
                "Inntekt i inntektsmelding er ulik inntekt fra A-inntekt, og ingen endringsårsak er oppgitt. Gjennomsnittlig inntekt fra A-inntekt: %s, oppgitt inntekt i inntektsmelding: %s",
                inntektFraAInntekt.gjennomsnitt(),
                inntektsmelding.getMånedInntekt());
            //Todo legger inn null i status inntil vi har håndtering av nedetid inntektskomponenten på plass
            return new SendInntektsmeldingResponse(false,
                null, null,
                new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.ULIK_INNTEKT, feilmelding, forespørsel.uuid().toString()));
        }

        loggTilfellerMedLikInntektOgHarÅrsak(inntektsmelding, inntektFraAInntekt.gjennomsnitt());
        return new SendInntektsmeldingResponse(true, null, null, null);
    }

    private static boolean erOppgittInntektUgyldig(InntektsmeldingDto inntektsmelding, Inntektsopplysninger inntektFraAInntekt) {
        return inntektFraAInntekt.gjennomsnitt().subtract(inntektsmelding.getMånedInntekt()).abs().compareTo(AKSEPTERT_AVVIK) > 0
                && (inntektsmelding.getEndringAvInntektÅrsaker() == null || inntektsmelding.getEndringAvInntektÅrsaker().isEmpty());
    }

    private void loggTilfellerMedLikInntektOgHarÅrsak(InntektsmeldingDto inntektsmelding, BigDecimal gjennomsnittligInntekt) {
        var inntektFraIm = inntektsmelding.getMånedInntekt();
        var likInntektMedÅrsak = inntektFraIm.compareTo(gjennomsnittligInntekt) == 0
            && inntektsmelding.getEndringAvInntektÅrsaker() != null && !inntektsmelding.getEndringAvInntektÅrsaker().isEmpty();
        if (likInntektMedÅrsak) {
            LOG.info("LIK_INNTEKT_OG_ÅRSAK: inntekt oppgitt av arbeidsgiver: {} er helt lik gjennomsnittlig inntekt fra a-inntekt. {}, og årsak(er) er oppgitt {}", inntektsmelding.getMånedInntekt(), gjennomsnittligInntekt, inntektsmelding.getEndringAvInntektÅrsaker());
        } else {
            var likInntektMedDifferanseOgÅrsak =
                gjennomsnittligInntekt.subtract(inntektFraIm).abs().compareTo(AKSEPTERT_AVVIK) > 0
                    && inntektsmelding.getEndringAvInntektÅrsaker() != null && !inntektsmelding.getEndringAvInntektÅrsaker().isEmpty();
            if (likInntektMedDifferanseOgÅrsak) {
                LOG.info(
                    "LIK_INNTEKT_INNENFOR_DIFFERANSE: inntekt oppgitt av arbeidsgiver: {} er lik gjennomsnittlig inntekt fra a-inntekt. {} med en margin på {} kroner. Endringsårsak(er) oppgitt: {}",
                    inntektsmelding.getMånedInntekt(),
                    gjennomsnittligInntekt,
                    AKSEPTERT_AVVIK,
                    inntektsmelding.getEndringAvInntektÅrsaker());
            }
        }
    }

    private boolean inntektsmeldingerErLike(InntektsmeldingDto nyInntektsmelding, InntektsmeldingDto tidligereInntektsmelding) {
        return Objects.equals(nyInntektsmelding.getStartdato(), tidligereInntektsmelding.getStartdato())
            && Objects.equals(nyInntektsmelding.getKontaktperson(), tidligereInntektsmelding.getKontaktperson())
            && erBeløpLike(nyInntektsmelding.getMånedInntekt(),tidligereInntektsmelding.getMånedInntekt())
            && erBeløpLike(nyInntektsmelding.getMånedRefusjon(),tidligereInntektsmelding.getMånedRefusjon())
            && refusjonsendringerErLike(nyInntektsmelding.getSøkteRefusjonsperioder(), tidligereInntektsmelding.getSøkteRefusjonsperioder())
            && naturalytelserErLike(nyInntektsmelding.getBortfaltNaturalytelsePerioder(), tidligereInntektsmelding.getBortfaltNaturalytelsePerioder())
            && Objects.equals(nyInntektsmelding.getYtelse(), tidligereInntektsmelding.getYtelse())
            && Objects.equals(nyInntektsmelding.getOpphørsdatoRefusjon(), tidligereInntektsmelding.getOpphørsdatoRefusjon())
            && endringsårsakerErLike(nyInntektsmelding.getEndringAvInntektÅrsaker(), tidligereInntektsmelding.getEndringAvInntektÅrsaker());
    }

    private boolean erBeløpLike(BigDecimal beløp1, BigDecimal beløp2) {
        if (beløp1 == null || beløp2 == null) {
            return beløp1 == null && beløp2 == null;
        }
        return beløp1.compareTo(beløp2) == 0;
    }

    private boolean refusjonsendringerErLike(List<InntektsmeldingDto.SøktRefusjon> nyListe,
                                             List<InntektsmeldingDto.SøktRefusjon> tidligereListe) {
        return Objects.equals(new HashSet<>(nyListe), new HashSet<>(tidligereListe));
    }

    private boolean naturalytelserErLike(List<InntektsmeldingDto.BortfaltNaturalytelse> nyListe,
                                         List<InntektsmeldingDto.BortfaltNaturalytelse> tidligereListe) {
        return Objects.equals(new HashSet<>(nyListe), new HashSet<>(tidligereListe));
    }

    private boolean endringsårsakerErLike(List<InntektsmeldingDto.Endringsårsak> nyListe,
                                          List<InntektsmeldingDto.Endringsårsak> tidligereListe) {
        return Objects.equals(new HashSet<>(nyListe), new HashSet<>(tidligereListe));
    }
}
