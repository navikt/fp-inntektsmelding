package no.nav.familie.inntektsmelding.imdialog.rest;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

public record HentArbeidsgiverRequest(@Valid @NotNull PersonIdent fødselsnummer,
                                         @Valid @NotNull Ytelsetype ytelseType,
                                     @Valid @NotNull LocalDate førsteFraværsdag) {
}
