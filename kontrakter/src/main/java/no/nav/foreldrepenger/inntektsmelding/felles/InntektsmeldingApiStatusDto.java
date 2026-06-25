package no.nav.foreldrepenger.inntektsmelding.felles;

/*Dette er inntektsmeldingsstatus vi eksponerer ut til konsumentene
fpinntektsmelding må vite om disse fordi mottatt kan være enten utdatert eller venter vurdering i vår database
 */
public enum InntektsmeldingApiStatusDto {
    AVVIST,
    MOTTATT,
    GODKJENT,
}
