package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record OpprettForespørselRequest(@NotNull @Valid AktørIdDto aktørId,
                                        @Valid OrganisasjonsnummerDto orgnummer,
                                        @NotNull @Valid LocalDate skjæringstidspunkt,
                                        @NotNull @Valid YtelseTypeDto ytelsetype,
                                        @NotNull @Valid SaksnummerDto fagsakSaksnummer,
                                        @Valid LocalDate førsteUttaksdato,
                                        @Valid List<OrganisasjonsnummerDto> organisasjonsnumre,
                                        @Valid boolean migrering) {
}
