package no.nav.foreldrepenger.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.SaksnummerDto;

public record LukkForespørselRequest(@Valid OrganisasjonsnummerDto orgnummer,
                                     @NotNull @Valid SaksnummerDto fagsakSaksnummer,
                                     @Valid LocalDate skjæringstidspunkt) {
}
