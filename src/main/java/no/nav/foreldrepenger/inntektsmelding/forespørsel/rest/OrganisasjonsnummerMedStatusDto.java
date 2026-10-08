package no.nav.foreldrepenger.inntektsmelding.forespørsel.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

public record OrganisasjonsnummerMedStatusDto(@NotNull @Valid OrganisasjonsnummerDto orgnummer, boolean erInntektsmeldingMottatt) {}
