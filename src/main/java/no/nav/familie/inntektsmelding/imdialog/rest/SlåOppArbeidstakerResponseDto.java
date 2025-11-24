package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;

import java.util.Set;

public record SlåOppArbeidstakerResponseDto(@NotNull String fornavn, String mellomnavn, @NotNull String etternavn, @NotNull Set<@Valid ArbeidsforholdDto> arbeidsforhold, @NotNull
                                            PersonInfo.Kjønn kjønn) {
    public record ArbeidsforholdDto(@NotNull String organisasjonsnavn, @NotNull String organisasjonsnummer) {}
}
