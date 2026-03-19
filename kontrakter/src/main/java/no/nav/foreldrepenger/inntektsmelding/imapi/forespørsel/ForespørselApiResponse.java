package no.nav.foreldrepenger.inntektsmelding.imapi.forespørsel;

import no.nav.foreldrepenger.inntektsmelding.felles.ForespørselStatusDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ForespørselApiResponse(UUID forespørselUuid,
                                     OrganisasjonsnummerDto orgnummer,
                                     String fødselsnummer,
                                     LocalDate førsteUttaksdato,
                                     LocalDate skjæringstidspunkt,
                                     ForespørselStatusDto status,
                                     YtelseTypeDto ytelseType,
                                     LocalDateTime opprettetTid) {
}
