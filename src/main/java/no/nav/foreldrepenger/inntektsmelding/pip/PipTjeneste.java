package no.nav.foreldrepenger.inntektsmelding.pip;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

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

    public OrganisasjonsnummerDto hentOrganisasjonsnummerFor(UUID forespørselUuid) {
        return forespørselTjeneste.hentForespørsel(forespørselUuid).map(f -> new OrganisasjonsnummerDto(f.getOrganisasjonsnummer())).orElse(null);
    }

    public OrganisasjonsnummerDto hentOrganisasjonsnummerFor(long inntektsmeldingId) {
        return Optional.ofNullable(inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId))
            .map(f -> new OrganisasjonsnummerDto(f.getArbeidsgiver().ident()))
            .orElse(null);
    }

    public OrganisasjonsnummerDto hentInntektsmeldingOrganisasjonsnummerFor(UUID inntektsmeldingUuid) {
        return Optional.ofNullable(inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingUuid))
            .map(im -> new OrganisasjonsnummerDto(im.getArbeidsgiver().ident()))
            .orElse(null);
    }

}
