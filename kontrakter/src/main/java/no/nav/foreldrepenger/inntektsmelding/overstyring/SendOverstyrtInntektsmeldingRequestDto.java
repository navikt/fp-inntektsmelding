package no.nav.foreldrepenger.inntektsmelding.overstyring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.felles.BortfaltNaturalytelseDto;
import no.nav.foreldrepenger.inntektsmelding.felles.RefusjonsendringDto;
import no.nav.foreldrepenger.inntektsmelding.felles.AktørIdDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.SaksnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;

public record SendOverstyrtInntektsmeldingRequestDto(@NotNull @Valid AktørIdDto aktorId,
                                                     @NotNull @Valid YtelseTypeDto ytelse,
                                                     @NotNull @Valid OrganisasjonsnummerDto arbeidsgiverIdent,
                                                     @NotNull LocalDate startdato,
                                                     @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
                                                     @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal refusjon,
                                                     @NotNull List<@Valid RefusjonsendringDto> refusjonsendringer,
                                                     @NotNull List<@Valid BortfaltNaturalytelseDto> bortfaltNaturalytelsePerioder,
                                                     @NotNull String opprettetAv,
                                                     @NotNull SaksnummerDto fagsystemSaksnummer) {

}

