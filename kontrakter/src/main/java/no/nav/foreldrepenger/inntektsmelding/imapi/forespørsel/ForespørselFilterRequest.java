package no.nav.foreldrepenger.inntektsmelding.imapi.forespørsel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.foreldrepenger.inntektsmelding.felles.ForespørselStatusDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;

import java.time.LocalDate;

public record ForespørselFilterRequest(@NotNull @Valid OrganisasjonsnummerDto orgnr,
                                       @Pattern(regexp = "^\\d{11}$") String fnr,
                                       @Valid ForespørselStatusDto status,
                                       @Valid YtelseTypeDto ytelseType,
                                       LocalDate fom,
                                       LocalDate tom) {
}
