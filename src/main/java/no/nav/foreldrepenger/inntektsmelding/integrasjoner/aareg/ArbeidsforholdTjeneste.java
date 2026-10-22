package no.nav.foreldrepenger.inntektsmelding.integrasjoner.aareg;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.aareg.dto.ArbeidsforholdDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.aareg.dto.OpplysningspliktigArbeidsgiverDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ArbeidsforholdType;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class ArbeidsforholdTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsforholdTjeneste.class);
    private AaregRestKlient aaregRestKlient;

    public ArbeidsforholdTjeneste() {
        // CDI
    }

    @Inject
    ArbeidsforholdTjeneste(AaregRestKlient aaregRestKlient) {
        this.aaregRestKlient = aaregRestKlient;
    }

    public List<Arbeidsforhold> hentArbeidsforhold(PersonIdent ident, LocalDate førsteFraværsdag) {
        return mapArbeidsforhold(ident, aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident.getIdent(), førsteFraværsdag));
    }

    public List<Arbeidsforhold> hentArbeidsforhold(PersonIdent ident, LocalDate fom, LocalDate tom) {
        return mapArbeidsforhold(ident, aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident.getIdent(), fom, tom));
    }

    private List<Arbeidsforhold> mapArbeidsforhold(PersonIdent ident, List<ArbeidsforholdDto> aaregInfo) {
        if (aaregInfo == null) {
            LOG.info("Fant ingen arbeidsforhold for person {}. Returnerer tom liste", ident);
            return Collections.emptyList();
        }
        LOG.info("Fant {} arbeidsforhold for person {}.", aaregInfo.size(), ident);

        return aaregInfo.stream()
            .filter(arb -> OpplysningspliktigArbeidsgiverDto.Type.ORGANISASJON.equals(arb.arbeidsgiver().type()))
            .map(arbeidsforholdDto -> new Arbeidsforhold(arbeidsforholdDto.arbeidsgiver().organisasjonsnummer(),
                mapAnsettelsesperiode(arbeidsforholdDto), mapType(arbeidsforholdDto)))
            .toList();
    }

    private static ArbeidsforholdType mapType(ArbeidsforholdDto arbeidsforholdDto) {
        if (arbeidsforholdDto.type() == null) {
            return null;
        }
        return switch (arbeidsforholdDto.type()) {
            case ORDINÆRT_ARBEIDSFORHOLD -> ArbeidsforholdType.ORDINÆRT_ARBEIDSFORHOLD;
            case MARITIMT_ARBEIDSFORHOLD -> ArbeidsforholdType.MARITIMT_ARBEIDSFORHOLD;
            case FORENKLET_OPPGJØRSORDNING -> ArbeidsforholdType.FORENKLET_OPPGJØRSORDNING;
            case FRILANSER_OPPDRAGSTAKER_MED_MER -> ArbeidsforholdType.FRILANSER_OPPDRAGSTAKER_MED_MER;
            case PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD -> ArbeidsforholdType.PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD;
        };
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
