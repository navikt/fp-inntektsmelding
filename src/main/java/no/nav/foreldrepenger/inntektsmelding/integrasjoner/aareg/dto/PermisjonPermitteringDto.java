package no.nav.foreldrepenger.inntektsmelding.integrasjoner.aareg.dto;

import java.math.BigDecimal;

public record PermisjonPermitteringDto(PeriodeDto periode, BigDecimal prosent, String type) {

}
