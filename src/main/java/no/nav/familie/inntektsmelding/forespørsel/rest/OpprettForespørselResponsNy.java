package no.nav.familie.inntektsmelding.forespørsel.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

import java.util.List;

public record OpprettForespørselResponsNy(@NotNull List<@Valid OrganisasjonsnummerMedStatus> organisasjonsnumreMedStatus) {
    public record OrganisasjonsnummerMedStatus(@NotNull @Valid OrganisasjonsnummerDto organisasjonsnummerDto, ForespørselResultat status) {}
}
