package no.nav.foreldrepenger.inntektsmelding.imdialog.rest.aginitiert;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

public record HentArbeidsgiverRequest(@Valid @NotNull PersonIdent fødselsnummer,
                                      @Valid @NotNull Ytelsetype ytelseType,
                                      @Valid @NotNull LocalDate førsteFraværsdag) {
}
