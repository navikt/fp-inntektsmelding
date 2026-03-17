package no.nav.foreldrepenger.inntektsmelding.pip;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;

@ApplicationScoped
public class PipTjeneste {
    private ForespørselTjeneste forespørselTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    PipTjeneste() {
        // CDI proxy
    }

    @Inject
    public PipTjeneste(ForespørselTjeneste forespørselTjeneste, InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    public Arbeidsgiver hentArbeidsgiverFor(UUID forespørselUuid) {
        return forespørselTjeneste.hentForespørsel(forespørselUuid).map(ForespørselDto::arbeidsgiver).orElse(null);
    }

    public Arbeidsgiver hentArbeidsgiverFor(long inntektsmeldingId) {
        return Optional.ofNullable(inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId))
            .map(InntektsmeldingDto::getArbeidsgiver)
            .orElse(null);
    }

    public Arbeidsgiver hentInntektsmeldingOrganisasjonsnummerFor(UUID inntektsmeldingUuid) {
        return Optional.ofNullable(inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingUuid))
            .map(InntektsmeldingDto::getArbeidsgiver)
            .orElse(null);
    }

}
