package no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.felles.BortfaltNaturalytelseDto;
import no.nav.foreldrepenger.inntektsmelding.felles.EndringsårsakerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.RefusjonsendringDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record InntektsmeldingApiResponse(@NotNull UUID inntektsmeldingUuid,
                                         @NotNull @Valid UUID forespørselUuid,
                                         @NotNull String fnr,
                                         @NotNull @Valid YtelseTypeDto ytelseType,
                                         @NotNull @Valid OrganisasjonsnummerDto arbeidsgiver,
                                         @NotNull @Valid Kontaktperson kontaktperson,
                                         @NotNull LocalDate startdato,
                                         @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
                                         @NotNull InntektsmeldingApiResponse.Innsendingsårsak innsendingsårsak,
                                         @NotNull InntektsmeldingApiResponse.Innsendingstype innsendingstype,
                                         @NotNull LocalDateTime innsendtTidspunkt,
                                         @NotNull InntektsmeldingApiResponse.AvsenderSystem avsenderSystem,
                                         @NotNull List<@Valid RefusjonsendringDto> søkteRefusjonsperioder,
                                         @NotNull List<@Valid BortfaltNaturalytelseDto> bortfaltNaturalytelsePerioder,
                                         @NotNull List<@Valid EndringsårsakerDto> endringAvInntektÅrsaker
) {

    public record Kontaktperson(
        String telefonnummer,
        String navn
    ) {
    }

    public record AvsenderSystem(
        String systemNavn,
        String systemVersjon
    ) {
    }

    public enum Innsendingsårsak {
        NY,
        ENDRING
    }

    public enum Innsendingstype {
        FORESPURT,
        ARBEIDSGIVER_INITIERT
    }
}

