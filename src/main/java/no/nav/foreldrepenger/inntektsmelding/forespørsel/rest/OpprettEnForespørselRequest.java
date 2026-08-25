package no.nav.foreldrepenger.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.YtelseTypeDto;

public record OpprettEnForespørselRequest(@NotNull @Valid AktørIdDto aktørId,
                                         @NotNull @Valid OrganisasjonsnummerDto orgnummer,
                                         @NotNull @Valid LocalDate skjæringstidspunkt,
                                         @NotNull @Valid YtelseTypeDto ytelsetype,
                                         @NotNull @Valid SaksnummerDto fagsakSaksnummer,
                                         @Valid LocalDate førsteUttaksdato) {
}
