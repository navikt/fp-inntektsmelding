package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import static no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTekster.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

class ForespørselTeksterTest {

    @Test
    void lagSaksTittelTest() {
        var navn = "OLA NORDMANN";
        var fødselsdato = LocalDate.of(2021, 2, 1);

        var saksTittel = ForespørselTekster.lagSaksTittel(navn, fødselsdato);
        assertThat(saksTittel).isEqualTo(SAKSTITTEL.formatted(capitalizeFully(navn), fødselsdato.format(DATE_TIME_FORMATTER)));
    }

    @Test
    void lagOppgaveTekstTest() {
        var ytelse = Ytelsetype.FORELDREPENGER;
        var oppgaveTekst = ForespørselTekster.lagOppgaveTekst(ytelse);
        assertThat(oppgaveTekst).isEqualTo(OPPGAVE_TEKST_NY.formatted(ForespørselTekster.mapYtelsestypeNavn(ytelse)));
    }

    @Test
    void lagTilleggsInformasjon_EksternInnsending() {
        var now = LocalDate.now();
        var statusTekst = lagTilleggsInformasjon(LukkeÅrsak.EKSTERN_INNSENDING, now);
        assertThat(statusTekst).isEqualTo(TILLEGGSINFORMASJON_UTFØRT_EKSTERN.formatted(now.format(DATE_TIME_FORMATTER)));
    }

    @Test
    void lagTilleggsInformasjon_OrdinærInnsending() {
        var now = LocalDate.now();
        var statusTekst = lagTilleggsInformasjon(LukkeÅrsak.ORDINÆR_INNSENDING, now);
        assertThat(statusTekst).isEqualTo(TILLEGGSINFORMASJON_ORDINÆR.formatted(now.format(DATE_TIME_FORMATTER)));
    }

    @Test
    void lagTilleggsInformasjon_Utgått() {
        var iGår = LocalDate.now().minusDays(1);
        var statusTekst = lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT, iGår);
        assertThat(statusTekst).isEqualTo(TILLEGGSINFORMASJON_UTGÅTT.formatted(iGår.format(DATE_TIME_FORMATTER)));
    }

    @Test
    void legVarselTekstMedOrgnvOgNavn() {
        var testOrgNavn = "test org";
        var testOrgNr = "1234321";
        var varselTekst = lagVarselTekst(Ytelsetype.FORELDREPENGER, new Organisasjon(testOrgNavn, testOrgNr));

        assertThat(varselTekst).isNotEmpty()
            .startsWith(testOrgNavn.toUpperCase())
            .contains(testOrgNr)
            .contains(Ytelsetype.FORELDREPENGER.name().toLowerCase());
    }

    @Test
    void legPåminnelseTekstMedOrgnvOgNavn() {
        var testOrgNavn = "org test org";
        var testOrgNr = "6531342";
        var varselTekst = lagPåminnelseTekst(Ytelsetype.SVANGERSKAPSPENGER, new Organisasjon(testOrgNavn, testOrgNr));

        assertThat(varselTekst).isNotEmpty()
            .startsWith(testOrgNavn.toUpperCase())
            .contains(testOrgNr)
            .contains(Ytelsetype.SVANGERSKAPSPENGER.name().toLowerCase());
    }

    @Test
    void capitalizeFully_shouldCapitalizeEachWord() {
        var softAssertions = new SoftAssertions();
        softAssertions.assertThat(ForespørselTekster.capitalizeFully("ola nordmann")).isEqualTo("Ola Nordmann");
        softAssertions.assertThat(ForespørselTekster.capitalizeFully("OLA NORDMANN")).isEqualTo("Ola Nordmann");
        softAssertions.assertThat(ForespørselTekster.capitalizeFully("oLa nOrDmAnn")).isEqualTo("Ola Nordmann");
        softAssertions.assertThat(ForespørselTekster.capitalizeFully("")).isEmpty();
        softAssertions.assertThat(ForespørselTekster.capitalizeFully("single")).isEqualTo("Single");
        softAssertions.assertThat(ForespørselTekster.capitalizeFully("OLA MELLOM NORDMANN")).isEqualTo("Ola Mellom Nordmann");
        softAssertions.assertAll();
    }

    @Test
    void mapYtelsestypeNavn_shouldMapCorrectly() {
        assertThat(ForespørselTekster.mapYtelsestypeNavn(Ytelsetype.FORELDREPENGER)).isEqualTo("foreldrepenger");
        assertThat(ForespørselTekster.mapYtelsestypeNavn(Ytelsetype.SVANGERSKAPSPENGER)).isEqualTo("svangerskapspenger");
    }
}
