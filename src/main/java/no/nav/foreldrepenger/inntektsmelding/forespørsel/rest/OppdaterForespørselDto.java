package no.nav.foreldrepenger.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselAksjon;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

public record OppdaterForespørselDto(@NotNull LocalDate skjæringstidspunkt, @NotNull @Valid OrganisasjonsnummerDto orgnr, ForespørselAksjon aksjon) {
}
