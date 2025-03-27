package no.nav.familie.inntektsmelding.imdialog.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.ForespørselStatusDto;
import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record InntektsmeldingDialogDto(@Valid @NotNull PersonInfoResponseDto person,
                                       @Valid @NotNull OrganisasjonInfoResponseDto arbeidsgiver,
                                       @Valid @NotNull InnsenderDto innsender,
                                       @Valid @NotNull InntektsopplysningerDto inntektsopplysninger,
                                       @Valid @NotNull LocalDate skjæringstidspunkt,
                                       @Valid @NotNull YtelseTypeDto ytelse,
                                       @Valid UUID forespørselUuid,
                                       @Valid @NotNull ForespørselStatusDto forespørselStatus,
                                       @Valid @NotNull LocalDate førsteUttaksdato,
                                       @Valid @NotNull List<AnsettelsePeriodeDto> ansettelsePerioder) {
    public record PersonInfoResponseDto(@NotNull String fornavn, @NotNull String mellomnavn, @NotNull String etternavn, @NotNull String fødselsnummer,
                                        @NotNull String aktørId) {
    }

    public record OrganisasjonInfoResponseDto(@NotNull String organisasjonNavn, @NotNull String organisasjonNummer) {
    }

    public record InnsenderDto(@NotNull String fornavn, String mellomnavn, @NotNull String etternavn, String telefon) {
    }

    public record InntektsopplysningerDto(@Valid BigDecimal gjennomsnittLønn, @Valid @NotNull List<MånedsinntektDto> månedsinntekter) {
        public record MånedsinntektDto(@NotNull LocalDate fom, @NotNull LocalDate tom, BigDecimal beløp, @Valid @NotNull MånedslønnStatus status) {
        }
    }

    public record AnsettelsePeriodeDto(LocalDate fom, LocalDate tom) {
    }
}
