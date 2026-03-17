package no.nav.foreldrepenger.inntektsmelding.imdialog.rest;

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

import com.fasterxml.jackson.annotation.JsonInclude;

import no.nav.foreldrepenger.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ArbeidsgiverinitiertÅrsakDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.YtelseTypeDto;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InntektsmeldingResponseDto(
    @NotNull Long id,
    @NotNull @Valid UUID foresporselUuid, @NotNull @Valid AktørIdDto aktorId,
    @NotNull @Valid YtelseTypeDto ytelse, @NotNull @Valid OrganisasjonsnummerDto arbeidsgiverIdent,
    @NotNull @Valid SendInntektsmeldingRequestDto.KontaktpersonRequestDto kontaktperson, @NotNull LocalDate startdato,
    @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
    @NotNull LocalDateTime opprettetTidspunkt,
    @NotNull List<SendInntektsmeldingRequestDto.@Valid Refusjon> refusjon,
    @NotNull List<SendInntektsmeldingRequestDto.@Valid BortfaltNaturalytelseRequestDto> bortfaltNaturalytelsePerioder,
    @NotNull List<SendInntektsmeldingRequestDto.@Valid EndringsårsakerRequestDto> endringAvInntektÅrsaker,
    @Valid ArbeidsgiverinitiertÅrsakDto arbeidsgiverinitiertÅrsak
) {
}
