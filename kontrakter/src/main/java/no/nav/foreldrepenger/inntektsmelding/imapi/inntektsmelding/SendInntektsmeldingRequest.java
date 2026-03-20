package no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.foreldrepenger.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.foreldrepenger.inntektsmelding.felles.BortfaltNaturalytelseDto;
import no.nav.foreldrepenger.inntektsmelding.felles.EndringsårsakerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.KontaktpersonDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.SøktRefusjonDto;
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;

public record SendInntektsmeldingRequest(@NotNull @Valid UUID foresporselUuid,
                                         @Pattern(regexp = "^\\d{11}$") @NotNull String fødselsnummer,
                                         @NotNull @Valid OrganisasjonsnummerDto organisasjonsnummer,
                                         @NotNull LocalDate startdato,
                                         @NotNull YtelseTypeDto ytelseType,
                                         @NotNull @Valid KontaktpersonDto kontaktperson,
                                         //kan inntekt noen gang være 0?
                                         @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
                                         @NotNull List<@Valid SøktRefusjonDto> refusjon,
                                         @NotNull List<@Valid BortfaltNaturalytelseDto> bortfaltNaturalytelsePerioder,
                                         @NotNull List<@Valid EndringsårsakerDto> endringAvInntektÅrsaker,
                                         @NotNull @Valid AvsenderSystemDto avsenderSystem) {

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

}
