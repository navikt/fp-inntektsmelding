package no.nav.foreldrepenger.inntektsmelding.imapi.forespørsel;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.felles.ForespørselStatusDto;
import no.nav.foreldrepenger.inntektsmelding.felles.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;

public record ForespørselFilterRequest(@NotNull @Valid OrganisasjonsnummerDto orgnr,
                                       @Valid FødselsnummerDto fnr,
                                       @Valid ForespørselStatusDto status,
                                       @Valid YtelseTypeDto ytelseType,
                                       LocalDate fom,
                                       LocalDate tom) {
}
