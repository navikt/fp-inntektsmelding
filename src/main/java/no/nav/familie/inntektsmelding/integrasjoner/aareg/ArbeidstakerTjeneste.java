package no.nav.familie.inntektsmelding.integrasjoner.aareg;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.pip.AltinnTilgangTjeneste;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class ArbeidstakerTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidstakerTjeneste.class);
    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;
    private AltinnTilgangTjeneste altinnTilgangTjeneste;

    public ArbeidstakerTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidstakerTjeneste(ArbeidsforholdTjeneste arbeidsforholdTjeneste, AltinnTilgangTjeneste altinnTilgangTjeneste) {
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.altinnTilgangTjeneste = altinnTilgangTjeneste;
    }

    public List<Arbeidsforhold> finnSøkersArbeidsforholdSomArbeidsgiverHarTilgangTil(PersonIdent ident, LocalDate førsteFraværsdag) {
        var alleArbeidsforholdTilSøker = arbeidsforholdTjeneste.hentArbeidsforhold(ident, førsteFraværsdag);
        LOG.info("Fant {} arbeidsforhold i Aa-registeret for {}", alleArbeidsforholdTilSøker.size(), ident);

        if (alleArbeidsforholdTilSøker.isEmpty()) {
            LOG.warn("Fant ingen arbeidsforhold i Aa-registeret for {}", ident);
            return Collections.emptyList();
        }

        var arbeidsforholdArbeidsgiverHarTilgangTil = alleArbeidsforholdTilSøker
            .stream()
            .filter(dto -> altinnTilgangTjeneste.harTilgangTilBedriften(dto.organisasjonsnummer()))
            .toList();

        if (alleArbeidsforholdTilSøker.size() > arbeidsforholdArbeidsgiverHarTilgangTil.size()) {
            LOG.info("Innsender har tilgang til {} av {} arbeidsforhold for {}", arbeidsforholdArbeidsgiverHarTilgangTil.size(), alleArbeidsforholdTilSøker.size(), ident);
        }

        LOG.info("Returnerer informasjon om arbeidsforhold for {}", ident);
        return arbeidsforholdArbeidsgiverHarTilgangTil;
    }

    public List<OrganisasjonsnummerDto> finnOrganisasjonerArbeidsgiverHarTilgangTil(PersonIdent ident) {
        var organisasjoner = altinnTilgangTjeneste.hentBedrifterArbeidsgiverHarTilgangTil().stream()
            .map(OrganisasjonsnummerDto::new)
            .toList();

        LOG.info("Arbeidsgiver har tilgang til følgende organisasjoner {} for {}", organisasjoner, ident);

        return organisasjoner;
    }
}
