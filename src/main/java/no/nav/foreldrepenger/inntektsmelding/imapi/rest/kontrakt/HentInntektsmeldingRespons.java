package no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt;

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

public record HentInntektsmeldingRespons(
    @NotNull UUID inntektsmeldingUuid,
    @NotNull @Valid UUID forespørselUuid,
    @NotNull FødselsnummerDto fnr,
    @NotNull @Valid YtelseType ytelseType,
    @NotNull @Valid ArbeidsgiverDto arbeidsgiver,
    @NotNull @Valid Kontaktperson kontaktperson,
    @NotNull LocalDate startdato,
    @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
    @NotNull Innsendingsårsak innsendingsårsak,
    @NotNull Innsendingstype innsendingstype,
    @NotNull LocalDateTime innsendtTidspunkt,
    @NotNull AvsenderSystem avsenderSystem,
    @NotNull List<@Valid SøktRefusjon> søkteRefusjonsperioder,
    @NotNull List<@Valid BortfaltNaturalytelse> bortfaltNaturalytelsePerioder,
    @NotNull List<@Valid Endringsårsaker> endringAvInntektÅrsaker
) {

}

