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

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.FellesGrunnlagTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;

import no.nav.foreldrepenger.inntektsmelding.typer.dto.MånedslønnStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.FellesMottakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
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
            return new SendInntektsmeldingResponse(false, null, "Finner ikke forespørsel for uuid " + forespørselUuid);
        }

        if (ForespørselStatus.UTGÅTT.equals(forespørsel.status())) {
            LOG.info("Forespørsel har status utgått. Inntektsmelding kan ikke mottas. forespørselUuid: {}", forespørselUuid);
            return new SendInntektsmeldingResponse(false, null,"Kan ikke sende inn inntektsmelding når Forespørsel har status forkastet.");
        }

        var sisteInntektsmelding = inntektsmeldingTjeneste.hentSisteInntektsmelding(forespørsel.uuid());
        if (sisteInntektsmelding != null && inntektsmeldingerErLike(inntektsmelding, sisteInntektsmelding)) {
            LOG.info("Inntektsmelding avvises. Ingen endring på ny inntektsmelding sammenlignet med tidligere innsendt inntektsmelding. inntektsmeldingId: {}", inntektsmelding.getId());
            return new SendInntektsmeldingResponse(false, null,
                "Inntektsmelding avvises. Ingen endring på ny inntektsmelding sammenlignet med tidligere innsendt inntektsmelding.");
        }

        //Todo Avklaring: Hva skal vi gjøre om inntektskomponenten er nede og vi ikke får sjekket dette? La de sende inn, men sette til status forkastet med forklaring?
        var sendInntektsmeldingResponse = sjekkMånedInntektMotRapportertInntekt(forespørsel, inntektsmelding);
        if (!sendInntektsmeldingResponse.success()) {
            return sendInntektsmeldingResponse;
        }

        var lagretIm = fellesMottakTjeneste.lagreOgJournalførInntektsmelding(inntektsmelding, forespørsel);
        fellesMottakTjeneste.behandlerForespørsel(forespørsel, Optional.ofNullable(lagretIm.getInntektsmeldingUuid()));

        MetrikkerTjeneste.loggInnsendtInntektsmelding(lagretIm);

        return new SendInntektsmeldingResponse(true, lagretIm.getInntektsmeldingUuid(),"Inntektsmelding mottatt");
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
            throw new IllegalStateException("InntektsmeldingApiMottakTjeneste: utviklerfeil - får tom inntekt fra ainntekt");
        }

        var nedetidAInntekt = inntektFraAInntekt.måneder() != null && inntektFraAInntekt.måneder()
            .stream()
            .anyMatch(status -> MånedslønnStatus.NEDETID_AINNTEKT.equals(status.status()));

        if (nedetidAInntekt) {
            LOG.warn(
                "Inntektskomponenten har nedetid, og vi kan ikke verifisere inntekt i inntektsmeldingen mot A-inntekt. inntektsmeldingId: {}",
                inntektsmelding.getId());
            return new SendInntektsmeldingResponse(false, null, "Inntektskomponenten har nedetid, og vi kan ikke verifisere inntekt i inntektsmeldingen mot A-inntekt. Prøv igjen om litt.");
        }

        var inntektErUlikOgIngenÅrsakOppgitt =
            inntektFraAInntekt.gjennomsnitt().subtract(inntektsmelding.getMånedInntekt()).abs().compareTo(AKSEPTERT_AVVIK) > 0
                && (inntektsmelding.getEndringAvInntektÅrsaker() == null || inntektsmelding.getEndringAvInntektÅrsaker().isEmpty());

        if (inntektErUlikOgIngenÅrsakOppgitt) {
            var feilmelding = String.format(
                "Inntekt i inntektsmelding er ulik inntekt fra A-inntekt, og ingen endringsårsak er oppgitt. Gjennomsnittlig inntekt fra A-inntekt: %s, oppgitt inntekt i inntektsmelding: %s", inntektFraAInntekt.gjennomsnitt(), inntektsmelding.getMånedInntekt());
            return new SendInntektsmeldingResponse(false, null, feilmelding);
        }

        loggTilfellerMedLikInntektOgHarÅrsak(inntektsmelding, inntektFraAInntekt.gjennomsnitt());

        return new SendInntektsmeldingResponse(true, null, "inntektsmelding godkjent");
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
            && Objects.equals(nyInntektsmelding.getKontaktperson().navn(), tidligereInntektsmelding.getKontaktperson().navn())
            && Objects.equals(nyInntektsmelding.getKontaktperson().telefonnummer(), tidligereInntektsmelding.getKontaktperson().telefonnummer())
            && erMånedInntektLike(nyInntektsmelding.getMånedInntekt(),tidligereInntektsmelding.getMånedInntekt())
            && refusjonsendringerErLike(nyInntektsmelding.getSøkteRefusjonsperioder(), tidligereInntektsmelding.getSøkteRefusjonsperioder())
            && naturalytelserErLike(nyInntektsmelding.getBortfaltNaturalytelsePerioder(), tidligereInntektsmelding.getBortfaltNaturalytelsePerioder())
            && Objects.equals(nyInntektsmelding.getYtelse(), tidligereInntektsmelding.getYtelse())
            && Objects.equals(nyInntektsmelding.getOpphørsdatoRefusjon(), tidligereInntektsmelding.getOpphørsdatoRefusjon())
            && endringsårsakerErLike(nyInntektsmelding.getEndringAvInntektÅrsaker(), tidligereInntektsmelding.getEndringAvInntektÅrsaker());
    }

    private boolean erMånedInntektLike(BigDecimal månedInntekt, BigDecimal månedInntekt1) {
        return månedInntekt.compareTo(månedInntekt1) == 0;
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
