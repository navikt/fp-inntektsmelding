package no.nav.familie.inntektsmelding.integrasjoner.aareg;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.OpplysningspliktigArbeidsgiverDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;

@ApplicationScoped
public class ArbeidsforholdTjeneste {
    private AaregRestKlient aaregRestKlient;
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsforholdTjeneste.class);

    public ArbeidsforholdTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsforholdTjeneste(AaregRestKlient aaregRestKlient) {
        this.aaregRestKlient = aaregRestKlient;
    }

    public List<Arbeidsforhold> hentArbeidsforhold(PersonIdent ident, LocalDate førsteFraværsdag) {
        var aaregInfo = aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident.getIdent(), førsteFraværsdag);
        if (aaregInfo == null) {
            LOG.info("Fant ingen arbeidsforhold for ident {}. Returnerer tom liste", ident.getIdent());
            return Collections.emptyList();
        }
        LOG.info("Fant {} arbeidsforhold for ident {}.", aaregInfo.size(), ident.getIdent());
        return aaregInfo.stream()
            .filter(arb -> OpplysningspliktigArbeidsgiverDto.Type.ORGANISASJON.equals(arb.arbeidsgiver().type())) // Vi skal aldri behandle private arbeidsforhold i ftinntektsmelding
            .map(arbeidsforhold -> new Arbeidsforhold(
                arbeidsforhold.arbeidsgiver().organisasjonsnummer(),
                arbeidsforhold.arbeidsforholdId()
            )).toList();
    }
}
