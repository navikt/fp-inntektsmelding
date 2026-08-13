package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;

public class ForespørselValiderer {

    private ForespørselValiderer() {
        // Skjuler default konstruktør
    }

    public static void validerStartdato(ForespørselDto forespørsel, LocalDate startdato) {
        if (!forespørsel.førsteUttaksdato().equals(startdato)) {
            throw new IllegalStateException("Startdato var ikke like");
        }
    }

    public static void validerOrganisasjon(ForespørselDto forespørsel, Arbeidsgiver arbeidsgiver) {
        if (!forespørsel.arbeidsgiver().equals(arbeidsgiver)) {
            throw new IllegalStateException("Organisasjonsnummer var ikke like");
        }
    }

    public static void validerAktør(ForespørselDto forespørsel, AktørId aktorId) {
        if (!forespørsel.aktørId().equals(aktorId)) {
            throw new IllegalStateException("AktørId for bruker var ikke like");
        }
    }
}
