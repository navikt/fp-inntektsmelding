package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;

public record ArbeidsgiverMedStatus(Arbeidsgiver arbeidsgiver, boolean erInntektsmeldingMottatt) {
}
