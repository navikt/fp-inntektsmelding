package no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.felles.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;

public record InntektsmeldingFilterRequest(@NotNull @Valid OrganisasjonsnummerDto orgnr,
                                           @Valid FødselsnummerDto fnr,
                                           @Valid YtelseTypeDto ytelseType,
                                           @Valid UUID forespørselUuid,
                                           @Valid LocalDate fom,
                                           @Valid LocalDate tom) {
}
