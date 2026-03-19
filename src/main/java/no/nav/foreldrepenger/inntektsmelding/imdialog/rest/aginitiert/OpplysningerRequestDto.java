package no.nav.foreldrepenger.inntektsmelding.imdialog.rest.aginitiert;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

import java.time.LocalDate;

public record OpplysningerRequestDto(@Valid @NotNull PersonIdent fødselsnummer,
                                     @Valid @NotNull Ytelsetype ytelseType,
                                     @Valid @NotNull LocalDate førsteFraværsdag,
                                     @Valid @NotNull OrganisasjonsnummerDto organisasjonsnummer) {
}
