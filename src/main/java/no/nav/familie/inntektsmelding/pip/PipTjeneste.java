package no.nav.familie.inntektsmelding.pip;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

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
            .map(f -> new OrganisasjonsnummerDto(f.getArbeidsgiverIdent()))
            .orElse(null);
    }

    public OrganisasjonsnummerDto hentInntektsmeldingOrganisasjonsnummerFor(UUID inntektsmeldingUuid) {
        return inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingUuid)
            .map(im -> new OrganisasjonsnummerDto(im.getArbeidsgiverIdent()))
            .orElse(null);
    }

}
