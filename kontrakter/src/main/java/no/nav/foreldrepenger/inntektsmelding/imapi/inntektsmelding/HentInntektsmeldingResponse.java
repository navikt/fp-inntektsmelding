package no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding;

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

import no.nav.foreldrepenger.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.foreldrepenger.inntektsmelding.felles.BortfaltNaturalytelseDto;
import no.nav.foreldrepenger.inntektsmelding.felles.EndringsårsakerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.InnsendingstypeDto;
import no.nav.foreldrepenger.inntektsmelding.felles.InnsendingsårsakDto;
import no.nav.foreldrepenger.inntektsmelding.felles.KontaktpersonDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.SøktRefusjonDto;
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;

public record HentInntektsmeldingResponse(
    @NotNull UUID inntektsmeldingUuid,
    @NotNull UUID forespørselUuid,
    @NotNull @Valid FødselsnummerDto fnr,
    @NotNull @Valid YtelseTypeDto ytelseType,
    @NotNull @Valid OrganisasjonsnummerDto arbeidsgiver,
    @NotNull @Valid KontaktpersonDto kontaktperson,
    @NotNull LocalDate startdato,
    @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
    @NotNull @Valid InnsendingsårsakDto innsendingsårsak,
    @NotNull @Valid InnsendingstypeDto innsendingstype,
    @NotNull LocalDateTime innsendtTidspunkt,
    @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal refusjonPrMnd,
    LocalDate opphørsdatoRefusjon,
    @NotNull @Valid AvsenderSystemDto avsenderSystem,
    @NotNull List<@Valid SøktRefusjonDto> refusjonsendringer,
    @NotNull List<@Valid BortfaltNaturalytelseDto> bortfaltNaturalytelsePerioder,
    @NotNull List<@Valid EndringsårsakerDto> endringAvInntektÅrsaker
) {

}

