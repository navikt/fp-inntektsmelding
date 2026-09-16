package no.nav.foreldrepenger.inntektsmelding.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.InntektsmeldingStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.exception.TekniskException;

/**
 * Kontrollerer oppgitt månedsinntekt i en inntektsmelding mot A-inntekt. Brukes av både den
 * eksterne (imapi/LPS) og den interaktive (imdialog) innsendingsflyten, samt av etterkontroll
 * av inntektsmeldinger som ble mottatt mens A-inntekt hadde nedetid.
 */
@ApplicationScoped
public class InntektKontrollTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektKontrollTjeneste.class);
    private static final BigDecimal AKSEPTERT_AVVIK = new BigDecimal("50");

    private InntektTjeneste inntektTjeneste;
    private PersonTjeneste personTjeneste;
    private FellesGrunnlagTjeneste fellesGrunnlagTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private FellesMottakTjeneste fellesMottakTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    InntektKontrollTjeneste() {
        // CDI
    }

    @Inject
    public InntektKontrollTjeneste(InntektTjeneste inntektTjeneste,
                                   PersonTjeneste personTjeneste,
                                   FellesGrunnlagTjeneste fellesGrunnlagTjeneste,
                                   InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                   FellesMottakTjeneste fellesMottakTjeneste,
                                   ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.inntektTjeneste = inntektTjeneste;
        this.personTjeneste = personTjeneste;
        this.fellesGrunnlagTjeneste = fellesGrunnlagTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.fellesMottakTjeneste = fellesMottakTjeneste;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    /**
     * Kontrollerer oppgitt månedsinntekt i en nylig mottatt (ikke ennå lagret) inntektsmelding mot A-inntekt.
     * Gjør ingen sideeffekter (lagring/varsling) - det er kallers ansvar å håndtere resultatet.
     */
    public InntektKontrollResultat sjekkInntektMotAInntekt(ForespørselDto forespørsel, InntektsmeldingDto inntektsmelding) {
        var inntektFraAInntekt = hentInntektFraAInntekt(forespørsel.aktørId(), forespørsel.ytelseType(), forespørsel.skjæringstidspunkt(),
            forespørsel.arbeidsgiver());

        if (inntektFraAInntekt == null) {
            LOG.warn(
                "InntektTjeneste har ikke returnert inntekt, og vi kan ikke verifisere inntekt i inntektsmeldingen mot A-inntekt. inntektsmeldingId: {}",
                inntektsmelding.getId());
            throw new IllegalStateException("InntektKontrollTjeneste: utviklerfeil - får tom inntekt fra A-inntekt");
        }

        if (inntektFraAInntekt.harNedetid()) {
            LOG.warn(
                "Inntektskomponenten har nedetid, og vi kan ikke verifisere inntekt i inntektsmeldingen mot A-inntekt. inntektsmeldingId: {}",
                inntektsmelding.getId());
            return new InntektKontrollResultat.Nedetid(
                "Inntektskomponenten har nedetid, og vi kan ikke verifisere inntekt i inntektsmeldingen mot A-inntekt. "
                    + "Vi prøver igjen om litt. Resultatet vil publiseres i Altinn og på arbeidsgivers side på nav.no når A-inntekt er oppe igjen.");
        }

        if (erOppgittInntektUgyldig(inntektsmelding, inntektFraAInntekt)) {
            var feilmelding = String.format(
                "Inntekt i inntektsmelding er ulik inntekt fra A-inntekt, og ingen endringsårsak er oppgitt. Gjennomsnittlig inntekt fra A-inntekt: %s, oppgitt inntekt i inntektsmelding: %s",
                inntektFraAInntekt.gjennomsnitt(),
                inntektsmelding.getMånedInntekt());
            return new InntektKontrollResultat.UlikInntekt(feilmelding, inntektFraAInntekt);
        }

        loggTilfellerMedLikInntektOgHarÅrsak(inntektsmelding, inntektFraAInntekt.gjennomsnitt());
        return new InntektKontrollResultat.Godkjent(inntektFraAInntekt);
    }

    /**
     * Etterkontroll av en inntektsmelding som tidligere ble mottatt med status VENTER_VURDERING pga.
     * nedetid i A-inntekt.
     */
    public void kontrollerInntektsmeldingEtterNedetid(Long inntektsmeldingId) {
        var inntektsmelding = inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId);
        if (InntektsmeldingStatus.UTDATERT.equals(inntektsmelding.getStatus())) {
            LOG.info("Inntektsmelding {} er utdatert, hopper over etterkontroll etter nedetid", inntektsmelding.getInntektsmeldingUuid());
            return;
        }
        var forespørsel = inntektsmelding.getForespørsel().orElseThrow();
        var inntekter = hentInntektFraAInntekt(inntektsmelding.getAktørId(), inntektsmelding.getYtelse(), forespørsel.skjæringstidspunkt(),
            inntektsmelding.getArbeidsgiver());

        if (inntekter == null) {
            LOG.warn(
                "InntektTjeneste har ikke returnert inntekt, og vi kan ikke etterkontrollere inntektsmelding mot A-inntekt. inntektsmeldingId: {}",
                inntektsmeldingId);
            throw new TekniskException("F-523043", "Får ikke hentet inntekt fra A-inntekt, får ikke ferdigstilt inntektsmelding " + inntektsmeldingId);
        }

        if (inntekter.harNedetid()) {
            //task feiler, vi oppdaterer status til venter vurdering
            inntektsmeldingTjeneste.oppdatertStatusTilInntektsmelding(inntektsmelding.getInntektsmeldingUuid(), InntektsmeldingStatus.VENTER_VURDERING);
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

    private Inntektsopplysninger hentInntektFraAInntekt(AktørId aktørId, Ytelsetype ytelseType, LocalDate skjæringstidspunkt, Arbeidsgiver arbeidsgiver) {
        var personInfo = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelseType);
        var harJobbetHeleBeregningsperioden = fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(personInfo, skjæringstidspunkt, arbeidsgiver);
        return inntektTjeneste.hentInntekt(aktørId, skjæringstidspunkt, LocalDate.now(), arbeidsgiver, harJobbetHeleBeregningsperioden);
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
                gjennomsnittligInntekt.subtract(inntektFraIm).abs().compareTo(AKSEPTERT_AVVIK) == 0
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
}
