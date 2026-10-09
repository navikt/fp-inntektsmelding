package no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak;

/**
 * MIDLERTIDIG. Speiler samme enum i fp-sak ({@code ForespørselVurderingResultat} i
 * {@code FordelRestTjeneste.forespørselStatus}-kontrakten). Brukes av en engangs ryddejobb for å avgjøre hva som
 * skal gjøres med en forespørsel:
 * <ul>
 *     <li>{@link #SETT_TIL_UTGÅTT}: saken er avsluttet, eller det er ingen påkrevde inntektsmeldinger for
 *     orgnummeret. Forespørselen(e) settes til utgått.</li>
 *     <li>{@link #SETT_TIL_FERDIG}: saken løper fortsatt og har påkrevde inntektsmeldinger, men alle er allerede
 *     mottatt. Forespørselen(e) settes til ferdig (ikke utgått), slik at arbeidsgiver fortsatt kan sende inn en
 *     (ny) inntektsmelding så lenge saken løper.</li>
 *     <li>{@link #TRENGER_FORTSATT_INNTEKTSMELDING}: minst én påkrevd inntektsmelding mangler fortsatt. Nyeste
 *     forespørsel beholdes, eventuelle eldre duplikater lukkes.</li>
 * </ul>
 */
public enum ForespørselVurderingResultat {
    TRENGER_FORTSATT_INNTEKTSMELDING,
    SETT_TIL_UTGÅTT,
    SETT_TIL_FERDIG
}
