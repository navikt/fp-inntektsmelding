package no.nav.foreldrepenger.inntektsmelding.integrasjoner.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.Folkeregisteridentifikator;
import no.nav.pdl.HentIdenterBolkResult;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjon;
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
    void finnPersonIdentForAktørIdBolk_skal_returnere_map_med_aktørId_til_personIdent() {
        var aktørId1 = new AktørId("1000000000001");
        var aktørId2 = new AktørId("1000000000002");

        var result1 = new HentIdenterBolkResult();
        result1.setIdent("1000000000001");
        result1.setIdenter(List.of(new IdentInformasjon("12345678901", IdentGruppe.FOLKEREGISTERIDENT, false)));
        result1.setCode("ok");

        var result2 = new HentIdenterBolkResult();
        result2.setIdent("1000000000002");
        result2.setIdenter(List.of(new IdentInformasjon("98765432109", IdentGruppe.FOLKEREGISTERIDENT, false)));
        result2.setCode("ok");

        when(pdlKlientMock.hentIdenterBolkResults(any(), any())).thenReturn(List.of(result1, result2));

        var resultat = personTjeneste.finnPersonIdentForAktørIdBolk(Set.of(aktørId1, aktørId2));

        assertEquals(2, resultat.size());
        assertEquals(new PersonIdent("12345678901"), resultat.get(aktørId1));
        assertEquals(new PersonIdent("98765432109"), resultat.get(aktørId2));
    }

    @Test
    void finnPersonIdentForAktørIdBolk_skal_håndtere_at_noen_aktørIder_ikke_finnes() {
        var aktørId1 = new AktørId("1000000000001");
        var aktørId2 = new AktørId("1000000000002");

        var result1 = new HentIdenterBolkResult();
        result1.setIdent("1000000000001");
        result1.setIdenter(List.of(new IdentInformasjon("12345678901", IdentGruppe.FOLKEREGISTERIDENT, false)));
        result1.setCode("ok");

        var result2 = new HentIdenterBolkResult();
        result2.setIdent("1000000000002");
        result2.setIdenter(null);
        result2.setCode("not_found");

        when(pdlKlientMock.hentIdenterBolkResults(any(), any())).thenReturn(List.of(result1, result2));

        var resultat = personTjeneste.finnPersonIdentForAktørIdBolk(Set.of(aktørId1, aktørId2));

        assertEquals(1, resultat.size());
        assertEquals(new PersonIdent("12345678901"), resultat.get(aktørId1));
    }

    @Test
    void finnPersonIdentForAktørIdBolk_skal_returnere_tomt_map_ved_tom_input() {
        when(pdlKlientMock.hentIdenterBolkResults(any(), any())).thenReturn(List.of());

        var resultat = personTjeneste.finnPersonIdentForAktørIdBolk(Set.of());

        assertTrue(resultat.isEmpty());
    }
}
