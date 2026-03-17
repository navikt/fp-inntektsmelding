package no.nav.foreldrepenger.inntektsmelding.forespørsel.rest;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.YtelseTypeDto;

public record OppdaterForespørslerRequest(@NotNull @Valid AktørIdDto aktørId,
                                          @NotNull List<@Valid OppdaterForespørselDto> forespørsler,
                                          @NotNull YtelseTypeDto ytelsetype,
                                          @NotNull @Valid SaksnummerDto fagsakSaksnummer) {
}
