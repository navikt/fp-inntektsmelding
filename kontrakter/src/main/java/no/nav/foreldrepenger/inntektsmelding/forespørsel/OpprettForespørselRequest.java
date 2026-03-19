package no.nav.foreldrepenger.inntektsmelding.forespørsel;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.felles.AktørIdDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.SaksnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;

public record OpprettForespørselRequest(@NotNull @Valid AktørIdDto aktørId,
                                        @Valid OrganisasjonsnummerDto orgnummer,
                                        @NotNull @Valid LocalDate skjæringstidspunkt,
                                        @NotNull @Valid YtelseTypeDto ytelsetype,
                                        @NotNull @Valid SaksnummerDto fagsakSaksnummer,
                                        @Valid LocalDate førsteUttaksdato,
                                        List<@Valid OrganisasjonsnummerDto> organisasjonsnumre) {
}
