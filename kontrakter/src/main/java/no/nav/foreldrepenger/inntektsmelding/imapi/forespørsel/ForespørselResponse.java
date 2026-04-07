package no.nav.foreldrepenger.inntektsmelding.imapi.forespørsel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.felles.ForespørselStatusDto;
import no.nav.foreldrepenger.inntektsmelding.felles.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;

public record ForespørselResponse(UUID forespørselUuid,
                                  OrganisasjonsnummerDto orgnummer,
                                  FødselsnummerDto fødselsnummer,
                                  LocalDate førsteUttaksdato,
                                  LocalDate skjæringstidspunkt,
                                  ForespørselStatusDto status,
                                  YtelseTypeDto ytelseType,
                                  LocalDateTime opprettetTid) {
}
