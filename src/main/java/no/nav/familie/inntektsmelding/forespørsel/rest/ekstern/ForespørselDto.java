package no.nav.familie.inntektsmelding.forespørsel.rest.ekstern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselStatusDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record ForespørselDto(UUID forespørselUuid,
                             OrganisasjonsnummerDto orgnummer,
                             String fødselsnummer,
                             LocalDate førsteUttaksdato,
                             LocalDate skjæringstidspunkt,
                             ForespørselStatusDto status,
                             YtelseTypeDto ytelseType,
                             LocalDateTime opprettetTid) {
}
