package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ForespørselEndringHistorikkDto(LocalDate skjæringstidspunkt, LocalDate førsteUttaksdato, LocalDateTime opprettetTid) {}
