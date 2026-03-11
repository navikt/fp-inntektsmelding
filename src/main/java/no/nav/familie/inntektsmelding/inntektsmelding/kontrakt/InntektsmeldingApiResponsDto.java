package no.nav.familie.inntektsmelding.inntektsmelding.kontrakt;

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


@JsonInclude(JsonInclude.Include.NON_NULL)
public record InntektsmeldingApiResponsDto(
    @NotNull UUID inntektsmeldingUuid,
    @NotNull @Valid UUID forespørselUuid,
    @NotNull String fnr,
    @NotNull @Valid Ytelse ytelse,
    @NotNull @Valid ArbeidsgiverInformasjonDto arbeidsgiver,
    @NotNull @Valid Kontaktperson kontaktperson,
    @NotNull LocalDate startdato,
    @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
    @NotNull InntektsmeldingApiResponsDto.Innsendingsårsak innsendingsårsak,
    @NotNull InntektsmeldingApiResponsDto.Innsendingstype innsendingstype,
    @NotNull LocalDateTime innsendtTidspunkt,
    @NotNull InntektsmeldingApiResponsDto.AvsenderSystem avsenderSystem,
    @NotNull List<@Valid SøktRefusjon> søkteRefusjonsperioder,
    @NotNull List<@Valid BortfaltNaturalytelse> bortfaltNaturalytelsePerioder,
    @NotNull List<@Valid Endringsårsaker> endringAvInntektÅrsaker
) {

    public record SøktRefusjon(@NotNull LocalDate fom,
                               @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record BortfaltNaturalytelse(@NotNull LocalDate fom,
                                        LocalDate tom,
                                        @NotNull Naturalytelsetype naturalytelsetype,
                                        @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record Endringsårsaker(@NotNull @Valid Endringsårsak årsak,
                                  LocalDate fom,
                                  LocalDate tom,
                                  LocalDate bleKjentFom) {
    }

    public record Kontaktperson(
        String tlf,
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

