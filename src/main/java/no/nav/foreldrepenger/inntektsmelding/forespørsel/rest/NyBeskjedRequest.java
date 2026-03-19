package no.nav.foreldrepenger.inntektsmelding.forespørsel.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.SaksnummerDto;

public record NyBeskjedRequest(@Valid @NotNull OrganisasjonsnummerDto orgnummer,
                                       @NotNull @Valid SaksnummerDto fagsakSaksnummer) {
}
