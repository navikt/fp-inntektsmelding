package no.nav.foreldrepenger.inntektsmelding.integrasjoner.aareg.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ArbeidsavtaleDto(BigDecimal stillingsprosent, LocalDate sistLoennsendring, PeriodeDto gyldighetsperiode) {
}
