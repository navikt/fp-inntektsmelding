package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselResultat;

public record ArbeidsgiverMedResultat(Arbeidsgiver arbeidsgiver, ForespørselResultat resultat) {
}
