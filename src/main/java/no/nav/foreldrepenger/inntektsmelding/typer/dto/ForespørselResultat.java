package no.nav.foreldrepenger.inntektsmelding.typer.dto;

public enum ForespørselResultat {
    /** Forespørsel fantes ikke, men er blitt opprettet. Oppgave er opprettet og varsling sendt. */
    FORESPØRSEL_OPPRETTET,
    /** Forespørselen fantes allerede, ingen endring ble gjort */
    IKKE_OPPRETTET_FINNES_ALLEREDE,
    /** Forespørselen fantes fra før, men ble endret som følge av bestillingen */
    FORESPØRSEL_ENDRET,
    /** Forespørselen fantes ikke og er blitt opprettet, men ble opprettet i lukket tilstand for å ikke sende varsling.
     * Dette gjøres for å gjøre det mulig for arbeidsgiver å endre inntektsmeldinger de sendte inn før vi sendte ut forespørsel om inntektsmelding,
     * da det ikke lenger er mulig (som hovedregel) å sende inntektsmelding uten at det finnes en forespørsel */
    LUKKET_FORESPØRSEL_OPPRETTET,
}
