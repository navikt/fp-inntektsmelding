package no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record HentForespørselResponse(UUID forespørselUuid,
                                      ArbeidsgiverDto orgnummer,
                                      FødselsnummerDto fødselsnummer,
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
