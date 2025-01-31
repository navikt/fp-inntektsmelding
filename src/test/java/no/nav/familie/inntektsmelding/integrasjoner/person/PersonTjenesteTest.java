package no.nav.familie.inntektsmelding.integrasjoner.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.Navn;
import no.nav.pdl.Person;
import no.nav.pdl.Telefonnummer;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;

@ExtendWith(MockitoExtension.class)
class PersonTjenesteTest {
    @Mock
    private PdlKlient pdlKlientMock;

    private MockedStatic<KontekstHolder> kontekstHolderMock;

    private PersonTjeneste personTjeneste;

    @BeforeEach
    void setUp() {
        personTjeneste = new PersonTjeneste(pdlKlientMock);
        kontekstHolderMock = mockStatic(KontekstHolder.class);
    }

    @AfterEach
    void tearDown() {
        kontekstHolderMock.close();
    }

    @Test
    void hentInnloggetPerson_skal_returnere_innlogget_person() {
        var ytelseType = Ytelsetype.FORELDREPENGER;
        var pdlPerson = new Person();
        var pdlNavn = new Navn();
        pdlNavn.setFornavn("fornavn");
        pdlNavn.setMellomnavn("mellomnavn");
        pdlNavn.setEtternavn("etternavn");
        var pdlTelefonnummer = new Telefonnummer();
        pdlTelefonnummer.setLandskode("47");
        pdlTelefonnummer.setNummer("81549300");
        var pdlFødselsdato = new Foedselsdato();
        pdlFødselsdato.setFoedselsdato("1997-11-23");

        pdlPerson.setNavn(List.of(pdlNavn));
        pdlPerson.setTelefonnummer(List.of(pdlTelefonnummer));
        pdlPerson.setFoedselsdato(List.of(pdlFødselsdato));
        pdlPerson.setKjoenn(List.of());

        var kontekst = RequestKontekst.forRequest("11839798115", null, IdentType.EksternBruker, null, null, new HashSet<>());
        when(KontekstHolder.harKontekst()).thenReturn(true);
        when(KontekstHolder.getKontekst()).thenReturn(kontekst);
        when(pdlKlientMock.hentPerson(any(), any(), any())).thenReturn(pdlPerson);

        var person = personTjeneste.hentInnloggetPerson(ytelseType);

        assertEquals("fornavn", person.fornavn());
        assertEquals("mellomnavn", person.mellomnavn());
        assertEquals("etternavn", person.etternavn());
        assertEquals("4781549300", person.telefonnummer());
        assertEquals(PersonInfo.Kjønn.UKJENT, person.kjønn());
    }

    @Test
    void hentInnloggetPerson_skal_kaste_exception_om_man_ikke_har_kontekst() {
        var ytelseType = Ytelsetype.FORELDREPENGER;

        when(KontekstHolder.harKontekst()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> personTjeneste.hentInnloggetPerson(ytelseType));
    }

    @Test
    void hentInnloggetPerson_skal_kaste_exception_om_identtypen_ikke_er_ekstern() {
        var ytelseType = Ytelsetype.FORELDREPENGER;

        when(KontekstHolder.harKontekst()).thenReturn(true);
        var kontekst = RequestKontekst.forRequest("11839798115", null, IdentType.InternBruker, null, null, new HashSet<>());
        when(KontekstHolder.getKontekst()).thenReturn(kontekst);

        assertThrows(IllegalStateException.class, () -> personTjeneste.hentInnloggetPerson(ytelseType));
    }
}
