package no.nav.foreldrepenger.inntektsmelding.forespørsel;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.SaksnummerDto;

public record LukkForespørselRequest(@Valid OrganisasjonsnummerDto orgnummer,
                                     @NotNull @Valid SaksnummerDto fagsakSaksnummer,
                                     @Valid LocalDate skjæringstidspunkt) {
}
