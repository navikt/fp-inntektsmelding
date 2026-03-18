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
        return new ForespørselDto(
            entitet.getUuid(),
            Arbeidsgiver.fra(entitet.getOrganisasjonsnummer()),
            AktørId.fra(entitet.getAktørId().getAktørId()),
            entitet.getYtelseType(),
            entitet.getStatus(),
            entitet.getForespørselType(),
            entitet.getSkjæringstidspunkt().orElse(null),
            entitet.getFørsteUttaksdato(),
            entitet.getFagsystemSaksnummer().map(Saksnummer::new).orElse(null),
            entitet.getOpprettetTidspunkt(),
            entitet.getArbeidsgiverNotifikasjonSakId(),
            entitet.getOppgaveId().orElse(null),
            entitet.getDialogportenUuid().orElse(null)
        );
    }
}

