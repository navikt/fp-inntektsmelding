package no.nav.foreldrepenger.inntektsmelding.typer.kodeverk;

public enum InntektsmeldingStatus {
    /** Inntektsmeldingen avviker fra a-inntekt, har ikke årsak og ble avvist. */
    AVVIST,
    /** Inntektsmeldingen venter på at a-inntekt skal være tilgjengelig for kontroll. */
    VENTER_VURDERING,
    /** Inntektsmeldingen er kontrollert og godkjent mot a-inntekt. */
    GODKJENT,
    /** En nyere inntektsmelding kom inn før denne ble godkjent – denne forkastes. */
    UTDATERT,

}
