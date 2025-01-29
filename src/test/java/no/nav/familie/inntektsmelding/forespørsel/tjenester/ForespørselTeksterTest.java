package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

class ForespørselTeksterTest {

    @Test
    void lagSaksTittel() {
        String saksTittel = ForespørselTekster.lagSaksTittel("OLA NORDMANN", LocalDate.of(2021, 02, 1));
        assertEquals("Inntektsmelding for Ola Nordmann (01.02.21)", saksTittel);
    }

    @Test
    void lagOppgaveTekst() {
        String oppgaveTekst = ForespørselTekster.lagOppgaveTekst(Ytelsetype.FORELDREPENGER);
        assertEquals("Innsending av inntektsmelding for opplæringspenger", oppgaveTekst);
    }

    @Test
    void lagTilleggsInformasjon_EksternInnsending() {
        String statusTekst = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.EKSTERN_INNSENDING);
        assertEquals("Utført i Altinn eller i bedriftens lønns- og personalsystem", statusTekst);
    }

    @Test
    void lagTilleggsInformasjon_OrdinærInnsending() {
        String statusTekst = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.ORDINÆR_INNSENDING);
        assertEquals(null, statusTekst);
    }

    @Test
    void lagTilleggsInformasjon_Utgått() {
        String statusTekst = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT);
        assertEquals("Du trenger ikke lenger å sende denne inntektsmeldingen", statusTekst);
    }

    @Test
    void legVarselTekstMedOrgnvOgNavn() {
        var testOrgNavn = "test org";
        var testOrgNr = "1234321";
        var varselTekst = ForespørselTekster.lagVarselTekst(Ytelsetype.FORELDREPENGER, new Organisasjon(testOrgNavn, testOrgNr));

        assertThat(varselTekst).isNotEmpty()
            .startsWith(testOrgNavn.toUpperCase())
            .contains(testOrgNr)
            .contains(Ytelsetype.FORELDREPENGER.name().toLowerCase());
    }

    @Test
    void legPåminnelseTekstMedOrgnvOgNavn() {
        var testOrgNavn = "org test org";
        var testOrgNr = "6531342";
        var varselTekst = ForespørselTekster.lagPåminnelseTekst(Ytelsetype.SVANGERSKAPSPENGER, new Organisasjon(testOrgNavn, testOrgNr));

        assertThat(varselTekst).isNotEmpty()
            .startsWith(testOrgNavn.toUpperCase())
            .contains(testOrgNr)
            .contains(Ytelsetype.SVANGERSKAPSPENGER.name().toLowerCase());
    }
}
