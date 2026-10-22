package no.nav.foreldrepenger.inntektsmelding.inntektsmelding;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.aareg.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ArbeidsforholdType;

import java.time.LocalDate;

@ApplicationScoped
public class FellesGrunnlagTjeneste {
    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;

    FellesGrunnlagTjeneste() {
        //CDI
    }

    @Inject
    public FellesGrunnlagTjeneste(ArbeidsforholdTjeneste arbeidsforholdTjeneste) {
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
    }

    public boolean harJobbetHeleBeregningsperioden(PersonInfo personinfo, LocalDate skjæringstidspunkt, Arbeidsgiver arbeidsgiver) {
        var førsteDagIBeregningsperiode = skjæringstidspunkt.minusMonths(3).withDayOfMonth(1);
        return arbeidsforholdTjeneste.hentArbeidsforhold(personinfo.fødselsnummer(), skjæringstidspunkt).stream()
            .filter(af -> af.organisasjonsnummer().equals(arbeidsgiver.orgnr()))
            .filter(af -> ArbeidsforholdType.ORDINÆRT_ARBEIDSFORHOLD.equals(af.type()))
            .anyMatch(af -> af.ansettelsesperiode().fom().isBefore(førsteDagIBeregningsperiode));
    }
}
