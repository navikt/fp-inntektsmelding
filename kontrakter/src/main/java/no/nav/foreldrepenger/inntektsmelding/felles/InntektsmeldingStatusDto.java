package no.nav.foreldrepenger.inntektsmelding.felles;

public enum InntektsmeldingStatusDto {
    AVVIST,
    /** Inntektsmeldingen venter på at a-inntekt skal være tilgjengelig for kontroll. */
    VENTER_VURDERING,
    /** Inntektsmeldingen er kontrollert og godkjent mot a-inntekt. */
    GODKJENT,
    /** En nyere inntektsmelding kom inn før denne ble godkjent – denne forkastes. */
    UTDATERT,
}
