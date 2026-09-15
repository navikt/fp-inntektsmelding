package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.FlerspråkligTekst;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.Merkelapp;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;


public class ForespørselTekster {
    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy");

    private ForespørselTekster() {
        // Skjuler default
    }

    // ---------------------------------------------------------------------
    // Min side – arbeidsgiver (arbeidsgivernotifikasjon) - kun bokmål
    // ---------------------------------------------------------------------

    static final String SAKSTITTEL = "Inntektsmelding for %s (%s)";
    static final String TILLEGGSINFORMASJON_UTFØRT_EKSTERN = "Utført i Altinn eller i bedriftens lønns- og personalsystem for første fraværsdag %s";
    static final String TILLEGGSINFORMASJON_UTGÅTT = "Du trenger ikke lenger å sende inntektsmeldingen for første fraværsdag %s";
    static final String TILLEGGSINFORMASJON_ORDINÆR = "For første fraværsdag %s";
    static final String OPPGAVE_TEKST_NY = "Innsending av inntektsmelding for %s";

    private static final String VARSEL_TEKST = "%s - orgnr %s: En av dine ansatte har søkt om %s og vi trenger inntektsmelding for å behandle søknaden. Logg inn på Min side – arbeidsgiver hos Nav. Hvis dere sender inn via lønnssystem kan dere fortsette med dette.";
    private static final String BESKJED_FRA_SAKSBEHANDLER_TEKST = "Vi har ennå ikke mottatt inntektsmelding for %s. For at vi skal kunne behandle søknaden om %s, må inntektsmeldingen sendes inn så raskt som mulig.";
    private static final String BESKJED_OM_INNSENDT_INNTEKTSMELDING = "Innsendt inntektsmelding";
    private static final String BESKJED_OM_OPPDATERT_INNTEKTSMELDING = "Oppdatert inntektsmelding";
    private static final String VARSEL_FRA_SAKSBEHANDLER_TEKST = "%s - orgnr %s: Vi har ennå ikke mottatt inntektsmelding. For at vi skal kunne behandle søknaden om %s, må inntektsmeldingen sendes inn så raskt som mulig.";

    public static String lagTilleggsInformasjon(LukkeÅrsak årsak, LocalDate førsteUttaksdato) {
        return switch (årsak) {
            case EKSTERN_INNSENDING -> TILLEGGSINFORMASJON_UTFØRT_EKSTERN.formatted(førsteUttaksdato.format(DATE_TIME_FORMATTER));
            case UTGÅTT -> TILLEGGSINFORMASJON_UTGÅTT.formatted(førsteUttaksdato.format(DATE_TIME_FORMATTER));
            case ORDINÆR_INNSENDING -> lagTilleggsInformasjonOrdinær(førsteUttaksdato);
        };
    }

    public static String lagTilleggsInformasjonOrdinær(LocalDate førsteFraværsdag) {
        return TILLEGGSINFORMASJON_ORDINÆR.formatted(førsteFraværsdag.format(DATE_TIME_FORMATTER));
    }

    public static String lagOppgaveTekst(Ytelsetype ytelseType) {
        return String.format(OPPGAVE_TEKST_NY, mapYtelsestypeNavn(ytelseType));
    }

    public static String lagSaksTittel(String navn, LocalDate fødselsdato) {
        return String.format(SAKSTITTEL, capitalizeFully(navn), fødselsdato.format(DATE_TIME_FORMATTER));
    }

    public static String lagVarselTekst(Ytelsetype ytelsetype, Organisasjon org) {
        return String.format(VARSEL_TEKST, org.navn().toUpperCase(), org.orgnr(), mapYtelsestypeNavn(ytelsetype));
    }

    public static String lagPåminnelseTekst(Ytelsetype ytelsetype, Organisasjon org) {
        return String.format(VARSEL_TEKST, org.navn().toUpperCase(), org.orgnr(), mapYtelsestypeNavn(ytelsetype));
    }

    public static String lagBeskjedFraSaksbehandlerTekst(Ytelsetype ytelseType, String søkerNavn) {
        return String.format(BESKJED_FRA_SAKSBEHANDLER_TEKST, søkerNavn, mapYtelsestypeNavn(ytelseType));
    }

    public static String lagBeskjedOmKvitteringFørsteInnsendingTekst() {
        return BESKJED_OM_INNSENDT_INNTEKTSMELDING;
    }

    public static String lagBeskjedOmOppdatertInntektsmelding() {
        return BESKJED_OM_OPPDATERT_INNTEKTSMELDING;
    }

    public static String lagVarselFraSaksbehandlerTekst(Ytelsetype ytelsetype, Organisasjon org) {
        return String.format(VARSEL_FRA_SAKSBEHANDLER_TEKST, org.navn().toUpperCase(), org.orgnr(), mapYtelsestypeNavn(ytelsetype));
    }

    public static Merkelapp finnMerkelapp(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> Merkelapp.INNTEKTSMELDING_FP;
            case SVANGERSKAPSPENGER -> Merkelapp.INNTEKTSMELDING_SVP;
        };
    }

    static String mapYtelsestypeNavn(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> "foreldrepenger";
            case SVANGERSKAPSPENGER -> "svangerskapspenger";
        };
    }

    static String capitalizeFully(String input) {
        return Arrays.stream(input.toLowerCase().split("\\s+")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
    }

    // ---------------------------------------------------------------------
    // Dialogporten - bokmål, nynorsk og engelsk
    // ---------------------------------------------------------------------

    private static final FlerspråkligTekst SAKSTITTEL_MAL = new FlerspråkligTekst(
        "Inntektsmelding for %s (%s)",
        "Inntektsmelding for %s (%s)",
        "Income statement for %s (%s)");

    private static final FlerspråkligTekst BESKJED_FRA_SAKSBEHANDLER_MAL = new FlerspråkligTekst(
        "Vi har ennå ikke mottatt inntektsmelding for %s. For at vi skal kunne behandle søknaden om %s, må inntektsmeldingen sendes inn så raskt som mulig.",
        "Vi har enno ikkje motteke inntektsmelding for %s. For at vi skal kunne behandle søknaden om %s, må inntektsmeldinga sendast inn så raskt som mogleg.",
        "We have not yet received the income statement for %s. To process the application for %s, the income statement must be submitted as soon as possible.");

    private static final FlerspråkligTekst AVVIST_INNTEKT_MAL = new FlerspråkligTekst(
        "Inntekt i inntektsmelding er ulik inntekt fra A-inntekt, og ingen endringsårsak er oppgitt. Gjennomsnittlig inntekt fra A-inntekt: %s, oppgitt inntekt i inntektsmelding: %s",
        "Inntekt i inntektsmeldinga er ulik inntekt frå A-inntekt, og ingen endringsårsak er oppgitt. Gjennomsnittleg inntekt frå A-inntekt: %s, oppgitt inntekt i inntektsmeldinga: %s",
        "The income in the income statement differs from the income reported to A-ordningen, and no reason for the change has been given. Average income from A-ordningen: %s, income stated in the income statement: %s");

    private static final FlerspråkligTekst YTELSESTYPE_FORELDREPENGER = new FlerspråkligTekst("foreldrepenger", "foreldrepengar", "parental benefit");
    private static final FlerspråkligTekst YTELSESTYPE_SVANGERSKAPSPENGER = new FlerspråkligTekst("svangerskapspenger", "svangerskapspengar", "pregnancy benefit");

    /**
     * Flerspråklig variant av sakstittel, brukt for dialoger i Dialogporten.
     */
    public static FlerspråkligTekst lagSaksTittelFlerspråklig(String navn, LocalDate fødselsdato) {
        return SAKSTITTEL_MAL.formatted(capitalizeFully(navn), fødselsdato.format(DATE_TIME_FORMATTER));
    }

    /**
     * Flerspråklig variant av "beskjed fra saksbehandler" (purring), brukt for meldinger i Dialogporten.
     */
    public static FlerspråkligTekst lagBeskjedFraSaksbehandlerTekstFlerspråklig(Ytelsetype ytelseType, String søkerNavn) {
        var ytelsesnavn = mapYtelsestypeNavnFlerspråklig(ytelseType);
        return new FlerspråkligTekst(BESKJED_FRA_SAKSBEHANDLER_MAL.nb().formatted(søkerNavn, ytelsesnavn.nb()),
            BESKJED_FRA_SAKSBEHANDLER_MAL.nn().formatted(søkerNavn, ytelsesnavn.nn()),
            BESKJED_FRA_SAKSBEHANDLER_MAL.en().formatted(søkerNavn, ytelsesnavn.en()));
    }

    /**
     * Flerspråklig tekst for avvist inntektsmelding pga. avvik mot A-inntekt, brukt for meldinger i Dialogporten.
     */
    public static FlerspråkligTekst lagAvvistInntektTekstFlerspråklig(BigDecimal gjennomsnittFraAInntekt, BigDecimal oppgittInntekt) {
        return AVVIST_INNTEKT_MAL.formatted(gjennomsnittFraAInntekt, oppgittInntekt);
    }

    private static FlerspråkligTekst mapYtelsestypeNavnFlerspråklig(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> YTELSESTYPE_FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YTELSESTYPE_SVANGERSKAPSPENGER;
        };
    }
}
