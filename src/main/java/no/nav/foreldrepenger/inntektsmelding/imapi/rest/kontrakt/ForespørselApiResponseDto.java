package no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ForespørselApiResponseDto(UUID forespørselUuid,
                                        ArbeidsgiverInformasjonDto orgnummer,
                                        String fødselsnummer,
                                        LocalDate førsteUttaksdato,
                                        LocalDate skjæringstidspunkt,
                                        Status status,
                                        YtelseType ytelseType,
                                        LocalDateTime opprettetTid) {

    public enum Status {
        UNDER_BEHANDLING,
        FERDIG,
        UTGÅTT
    }

}
