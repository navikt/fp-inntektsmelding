package no.nav.familie.inntektsmelding.integrasjoner.aareg;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.ArbeidsforholdDto;
import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.OpplysningspliktigArbeidsgiverDto;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class ArbeidsforholdTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsforholdTjeneste.class);
    private AaregRestKlient aaregRestKlient;

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
            LOG.info("Fant ingen arbeidsforhold for ident {}. Returnerer tom liste", ident);
            return Collections.emptyList();
        }
        LOG.info("Fant {} arbeidsforhold for ident {}.", aaregInfo.size(), ident);

        return aaregInfo.stream()
            .filter(arb -> OpplysningspliktigArbeidsgiverDto.Type.ORGANISASJON.equals(arb.arbeidsgiver().type()))
            .map(arbeidsforholdDto -> new Arbeidsforhold(arbeidsforholdDto.arbeidsgiver().organisasjonsnummer(),
                mapAnsettelsesperiode(arbeidsforholdDto)))
            .toList();
    }

    private Arbeidsforhold.Ansettelsesperiode mapAnsettelsesperiode(ArbeidsforholdDto arbeidsforholdDto) {
        if (arbeidsforholdDto.ansettelsesperiode() == null) {
            return null;
        }
        var ansettelsesperiodeTom = arbeidsforholdDto.ansettelsesperiode().periode().tom() != null
                                    ? arbeidsforholdDto.ansettelsesperiode().periode().tom()
                                    : Tid.TIDENES_ENDE;

        return new Arbeidsforhold.Ansettelsesperiode(arbeidsforholdDto.ansettelsesperiode().periode().fom(), ansettelsesperiodeTom);
    }
}
