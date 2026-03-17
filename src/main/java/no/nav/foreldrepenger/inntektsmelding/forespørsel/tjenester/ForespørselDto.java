package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

public record ForespørselDto(UUID uuid,
                             Arbeidsgiver arbeidsgiver,
                             AktørId aktørId,
                             Ytelsetype ytelseType,
                             ForespørselStatus status,
                             ForespørselType forespørselType,
                             LocalDate skjæringstidspunkt,
                             LocalDate førsteUttaksdato,
                             String fagsystemSaksnummer,
                             LocalDateTime opprettetTidspunkt,
                             String arbeidsgiverNotifikasjonSakId,
                             String oppgaveId,
                             UUID dialogportenUuid) {
}

