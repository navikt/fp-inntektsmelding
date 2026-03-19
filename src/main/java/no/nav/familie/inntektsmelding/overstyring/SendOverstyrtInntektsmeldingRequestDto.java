package no.nav.familie.inntektsmelding.overstyring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.NaturalytelsetypeDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record SendOverstyrtInntektsmeldingRequestDto(@NotNull @Valid AktørIdDto aktorId,
                                                     @NotNull @Valid YtelseTypeDto ytelse,
                                                     @NotNull @Valid OrganisasjonsnummerDto arbeidsgiverIdent,
                                                     @NotNull LocalDate startdato,
                                                     @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
                                                     @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal refusjon,
                                                     @NotNull List<@Valid RefusjonendringRequestDto> refusjonsendringer,
                                                     @NotNull List<@Valid BortfaltNaturalytelseRequestDto> bortfaltNaturalytelsePerioder,
                                                     @NotNull String opprettetAv,
                                                     @NotNull SaksnummerDto fagsystemSaksnummer) {

    public record RefusjonendringRequestDto(@NotNull LocalDate fom,
                                            @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record BortfaltNaturalytelseRequestDto(@NotNull LocalDate fom,
                                                  LocalDate tom,
                                                  @NotNull NaturalytelsetypeDto naturalytelsetype,
                                                  @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }
}

