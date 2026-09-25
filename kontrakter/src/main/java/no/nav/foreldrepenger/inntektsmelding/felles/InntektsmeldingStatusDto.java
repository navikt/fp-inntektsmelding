package no.nav.foreldrepenger.inntektsmelding.felles;

public enum InntektsmeldingStatusDto {
    /** Inntektsmeldingen ble avvist, og den er ikke journalført. */
    AVVIST,
    /** Inntektsmeldingen venter på at a-inntekt skal være tilgjengelig for kontroll. */
    VENTER_VURDERING,
    /** Inntektsmeldingen er kontrollert mot a-inntekt, og ble godkjent. */
    GODKJENT,
    /** En nyere inntektsmelding kom inn før kontroll og ble forkastet. */
    UTDATERT,
}
