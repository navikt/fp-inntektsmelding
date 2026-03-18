package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;

public class ForespørselDtoMapper {

    private ForespørselDtoMapper() {
        // Skjuler default konstruktør
    }

    public static ForespørselDto mapFraEntitet(ForespørselEntitet entitet) {
        if (entitet == null) {
            return null;
        }
        return ForespørselDto.builder()
            .uuid(entitet.getUuid())
            .arbeidsgiver(Arbeidsgiver.fra(entitet.getOrganisasjonsnummer()))
            .aktørId(AktørId.fra(entitet.getAktørId().getAktørId()))
            .ytelseType(entitet.getYtelseType())
            .status(entitet.getStatus())
            .forespørselType(entitet.getForespørselType())
            .skjæringstidspunkt(entitet.getSkjæringstidspunkt().orElse(null))
            .førsteUttaksdato(entitet.getFørsteUttaksdato())
            .fagsystemSaksnummer(entitet.getFagsystemSaksnummer().map(Saksnummer::new).orElse(null))
            .opprettetTidspunkt(entitet.getOpprettetTidspunkt())
            .arbeidsgiverNotifikasjonSakId(entitet.getArbeidsgiverNotifikasjonSakId())
            .oppgaveId(entitet.getOppgaveId().orElse(null))
            .dialogportenUuid(entitet.getDialogportenUuid().orElse(null))
            .build();
    }
}

