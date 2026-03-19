package no.nav.familie.inntektsmelding.imdialog.rest.ekstern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.familie.inntektsmelding.typer.dto.EndringsårsakDto;
import no.nav.familie.inntektsmelding.typer.dto.NaturalytelsetypeDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

public record SendInntektsmeldingEksternRequest(@NotNull @Valid UUID foresporselUuid,
                                                @Pattern(
                                                    regexp = "^\\d{11}$"
                                                ) @NotNull String fødselsnummer,
                                                @NotNull @Valid OrganisasjonsnummerDto organisasjonsnummer,
                                                @NotNull LocalDate startdato,
                                                @NotNull YtelseTypeRequest ytelseType,
                                                @NotNull @Valid KontaktpersonRequest kontaktperson,
                                                //kan inntekt noen gang være 0?
                                                @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
                                                @NotNull List<@Valid RefusjonRequest> refusjon,
                                                @NotNull List<@Valid BortfaltNaturalytelseRequest> bortfaltNaturalytelsePerioder,
                                                @NotNull List<@Valid EndringsårsakerRequest> endringAvInntektÅrsaker,
                                                @NotNull @Valid AvsenderSystemRequest avsenderSystem) {

    @Override
    public String toString() {
        return "SendInntektsmeldingEksternRequest{" +
            "foresporselUuid=" + foresporselUuid +
            ", fødselsnummer='****" + fødselsnummer.substring(7) + "'" +
            ", organisasjonsnummer=" + organisasjonsnummer +
            ", startdato=" + startdato +
            ", ytelseType=" + ytelseType +
            ", kontaktperson=" + kontaktperson +
            ", inntekt=" + inntekt +
            ", refusjon=" + refusjon +
            ", bortfaltNaturalytelsePerioder=" + bortfaltNaturalytelsePerioder +
            ", endringAvInntektÅrsaker=" + endringAvInntektÅrsaker +
            ", avsenderSystem=" + avsenderSystem +
            '}';
    }

    public enum YtelseTypeRequest {
        FORELDREPENGER,
        SVANGERSKAPSPENGER
    }

    public record KontaktpersonRequest(@Size(max = 200) @NotNull String navn, @NotNull @Size(max = 100) String telefonnummer) {
    }

    public record RefusjonRequest(@NotNull LocalDate fom,
                                  @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record BortfaltNaturalytelseRequest(@NotNull LocalDate fom,
                                               LocalDate tom,
                                               @NotNull NaturalytelsetypeDto naturalytelsetype,
                                               @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record EndringsårsakerRequest(@NotNull @Valid EndringsårsakDto årsak,
                                         LocalDate fom,
                                         LocalDate tom,
                                         LocalDate bleKjentFom) {
    }

    public record AvsenderSystemRequest(@NotNull @Size(max = 200) String navn, @NotNull @Size(max = 100) String versjon, @NotNull LocalDateTime innsendtTidspunkt) {
    }
}
