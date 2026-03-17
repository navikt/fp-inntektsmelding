package no.nav.foreldrepenger.inntektsmelding.typer.dto;

/**
 * Hvorfor inntekt i inntektsmeldingen er endret fra snittet de siste tre måneder
 */
public enum EndringsårsakDto {
    PERMITTERING,
    NY_STILLING,
    NY_STILLINGSPROSENT,
    SYKEFRAVÆR,
    BONUS,
    FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER,
    NYANSATT,
    MANGELFULL_RAPPORTERING_AORDNING,
    INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING,
    TARIFFENDRING,
    FERIE,
    VARIG_LØNNSENDRING,
    PERMISJON
}
