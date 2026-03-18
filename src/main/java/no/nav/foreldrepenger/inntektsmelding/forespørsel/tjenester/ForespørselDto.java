package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
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
                             Saksnummer fagsystemSaksnummer,
                             LocalDateTime opprettetTidspunkt,
                             String arbeidsgiverNotifikasjonSakId,
                             String oppgaveId,
                             UUID dialogportenUuid) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID uuid;
        private Arbeidsgiver arbeidsgiver;
        private AktørId aktørId;
        private Ytelsetype ytelseType;
        private ForespørselStatus status;
        private ForespørselType forespørselType;
        private LocalDate skjæringstidspunkt;
        private LocalDate førsteUttaksdato;
        private Saksnummer fagsystemSaksnummer;
        private LocalDateTime opprettetTidspunkt;
        private String arbeidsgiverNotifikasjonSakId;
        private String oppgaveId;
        private UUID dialogportenUuid;

        public Builder uuid(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder arbeidsgiver(Arbeidsgiver arbeidsgiver) {
            this.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder aktørId(AktørId aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder ytelseType(Ytelsetype ytelseType) {
            this.ytelseType = ytelseType;
            return this;
        }

        public Builder status(ForespørselStatus status) {
            this.status = status;
            return this;
        }

        public Builder forespørselType(ForespørselType forespørselType) {
            this.forespørselType = forespørselType;
            return this;
        }

        public Builder skjæringstidspunkt(LocalDate skjæringstidspunkt) {
            this.skjæringstidspunkt = skjæringstidspunkt;
            return this;
        }

        public Builder førsteUttaksdato(LocalDate førsteUttaksdato) {
            this.førsteUttaksdato = førsteUttaksdato;
            return this;
        }

        public Builder fagsystemSaksnummer(Saksnummer fagsystemSaksnummer) {
            this.fagsystemSaksnummer = fagsystemSaksnummer;
            return this;
        }

        public Builder opprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
            this.opprettetTidspunkt = opprettetTidspunkt;
            return this;
        }

        public Builder arbeidsgiverNotifikasjonSakId(String arbeidsgiverNotifikasjonSakId) {
            this.arbeidsgiverNotifikasjonSakId = arbeidsgiverNotifikasjonSakId;
            return this;
        }

        public Builder oppgaveId(String oppgaveId) {
            this.oppgaveId = oppgaveId;
            return this;
        }

        public Builder dialogportenUuid(UUID dialogportenUuid) {
            this.dialogportenUuid = dialogportenUuid;
            return this;
        }

        public ForespørselDto build() {
            return new ForespørselDto(uuid, arbeidsgiver, aktørId, ytelseType, status, forespørselType,
                skjæringstidspunkt, førsteUttaksdato, fagsystemSaksnummer, opprettetTidspunkt,
                arbeidsgiverNotifikasjonSakId, oppgaveId, dialogportenUuid);
        }
    }
}
