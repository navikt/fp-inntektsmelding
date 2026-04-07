package no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SendInntektsmeldingRequest(@NotNull @Valid UUID foresporselUuid,
                                         @NotNull @Valid FødselsnummerDto fødselsnummer,
                                         @NotNull @Valid ArbeidsgiverDto organisasjonsnummer,
                                         @NotNull LocalDate startdato,
                                         @NotNull YtelseType ytelseType,
                                         @NotNull @Valid Kontaktperson kontaktperson,
                                         //kan inntekt noen gang være 0?
                                         @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
                                         @NotNull List<@Valid SøktRefusjon> refusjon,
                                         @NotNull List<@Valid BortfaltNaturalytelse> bortfaltNaturalytelsePerioder,
                                         @NotNull List<@Valid Endringsårsaker> endringAvInntektÅrsaker,
                                         @NotNull @Valid AvsenderSystem avsenderSystem) {
}
