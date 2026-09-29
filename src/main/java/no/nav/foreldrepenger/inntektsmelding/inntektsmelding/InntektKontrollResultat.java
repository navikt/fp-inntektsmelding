package no.nav.foreldrepenger.inntektsmelding.inntektsmelding;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;

/**
 * Resultat av å kontrollere en oppgitt månedsinntekt i en inntektsmelding mot A-inntekt.
 * Nøytral i forhold til hvem som kaller (imapi/imdialog), slik at begge innsendingsflytene
 * kan mappe resultatet til sin egen respons-kontrakt.
 */
public sealed interface InntektKontrollResultat {

    /** Oppgitt inntekt er innenfor akseptert avvik fra A-inntekt, eller avviket har oppgitt endringsårsak. */
    record Godkjent(Inntektsopplysninger inntektFraAInntekt) implements InntektKontrollResultat {
    }

    /** A-inntekt har nedetid og kan ikke brukes til å kontrollere oppgitt inntekt akkurat nå. */
    record Nedetid(String melding) implements InntektKontrollResultat {
    }

    /** Oppgitt inntekt avviker fra A-inntekt, og ingen endringsårsak er oppgitt. */
    record UlikInntekt(String feilmelding, Inntektsopplysninger inntektFraAInntekt) implements InntektKontrollResultat {
    }
}
