package no.nav.foreldrepenger.inntektsmelding.forespørsel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.SaksnummerDto;

public record NyBeskjedRequest(@Valid @NotNull OrganisasjonsnummerDto orgnummer,
                               @NotNull @Valid SaksnummerDto fagsakSaksnummer) {
}
