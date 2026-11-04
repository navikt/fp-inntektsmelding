package no.nav.foreldrepenger.inntektsmelding.integrasjoner.person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import no.nav.pdl.HentIdenterBolkResult;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjon;
import no.nav.pdl.Person;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.util.LRUCache;

@ExtendWith(MockitoExtension.class)
class PersonTjenesteTest {
    @Mock
    private PdlKlient pdlKlientMock;

    private MockedStatic<KontekstHolder> kontekstHolderMock;

    private PersonTjeneste personTjeneste;

    @BeforeEach
    void setUp() throws Exception {
        personTjeneste = new PersonTjeneste(pdlKlientMock);
        kontekstHolderMock = mockStatic(KontekstHolder.class);
        tømCache();
    }

    private static void tømCache() throws Exception {
        var cacheField = PersonTjeneste.class.getDeclaredField("CACHE_AKTØR_ID_TIL_IDENT");
        cacheField.setAccessible(true);
        var lruCache = (LRUCache<?, ?>) cacheField.get(null);
        var mapField = LRUCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        ((Map<?, ?>) mapField.get(lruCache)).clear();
    }

    @AfterEach
    void tearDown() {
        kontekstHolderMock.close();
    }

    @Test
    void skal_logge_manglende_aktiv_identifikator_maskert_kun_i_sikkerlogg() {
        var ident = new PersonIdent("12345678901");
        var aktørId = new AktørId("1000000000001");
        var person = mock(Person.class);
        when(person.getFolkeregisteridentifikator()).thenReturn(List.of());
        when(pdlKlientMock.hentAktørIdForPersonIdent(ident.getIdent(), true)).thenReturn(Optional.of(aktørId.getAktørId()));
        when(pdlKlientMock.hentPerson(any(), any(), any())).thenReturn(person);

        var logger = (Logger) LoggerFactory.getLogger(PersonTjeneste.class);
        var sikkerlogger = (Logger) LoggerFactory.getLogger("secureLogger");
        var vanligeHendelser = new ListAppender<ILoggingEvent>();
        var sikreHendelser = new ListAppender<ILoggingEvent>();
        vanligeHendelser.start();
        sikreHendelser.start();
        logger.addAppender(vanligeHendelser);
        sikkerlogger.addAppender(sikreHendelser);
        try {
            var feil = assertThrows(IllegalStateException.class,
                () -> personTjeneste.hentPersonFraIdent(ident, Ytelsetype.FORELDREPENGER));

            assertThat(feil.getMessage()).contains("Person uten aktiv Folkeregisteridentifikator", ident.toString())
                .doesNotContain(ident.getIdent(), aktørId.getAktørId());
            assertThat(sikreHendelser.list).singleElement().satisfies(hendelse -> {
                assertThat(hendelse.getLevel()).isEqualTo(Level.WARN);
                assertThat(hendelse.getThrowableProxy()).isNull();
                assertThat(hendelse.getFormattedMessage()).contains(ident.toString(), aktørId.toString())
                    .doesNotContain(ident.getIdent(), aktørId.getAktørId());
                assertThat(hendelse.getArgumentArray()).allSatisfy(argument ->
                    assertThat(String.valueOf(argument)).doesNotContain(ident.getIdent(), aktørId.getAktørId()));
            });
            assertThat(vanligeHendelser.list).allSatisfy(hendelse ->
                assertThat(hendelse.getFormattedMessage())
                    .doesNotContain("Person uten aktiv Folkeregisteridentifikator", ident.getIdent(), ident.toString()));
        } finally {
            logger.detachAppender(vanligeHendelser);
            sikkerlogger.detachAppender(sikreHendelser);
            vanligeHendelser.stop();
            sikreHendelser.stop();
        }
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

        var resultat = personTjeneste.finnPersonIdentForAktørIdBolk(Set.of());
        assertTrue(resultat.isEmpty());
    }

    @Test
    void skal_kalle_pdl_når_cache_er_tom() {
        var aktørId = new AktørId("9000000000001");
        var pdlResult = new HentIdenterBolkResult();
        pdlResult.setIdent("9000000000001");
        pdlResult.setIdenter(List.of(new IdentInformasjon("11111111111", IdentGruppe.FOLKEREGISTERIDENT, false)));
        pdlResult.setCode("ok");
        when(pdlKlientMock.hentIdenterBolkResults(any(), any())).thenReturn(List.of(pdlResult));

        var resultat = personTjeneste.finnPersonIdentForAktørIdBolk(Set.of(aktørId));

        assertEquals(new PersonIdent("11111111111"), resultat.get(aktørId));
        verify(pdlKlientMock, times(1)).hentIdenterBolkResults(any(), any());
    }

    @Test
    void skal_ikke_kalle_pdl_når_alle_er_i_cache() {
        var aktørId = new AktørId("9000000000002");
        var pdlResult = new HentIdenterBolkResult();
        pdlResult.setIdent("9000000000002");
        pdlResult.setIdenter(List.of(new IdentInformasjon("22222222222", IdentGruppe.FOLKEREGISTERIDENT, false)));
        pdlResult.setCode("ok");
        when(pdlKlientMock.hentIdenterBolkResults(any(), any())).thenReturn(List.of(pdlResult));

        // Første kall — populerer cachen
        personTjeneste.finnPersonIdentForAktørIdBolk(Set.of(aktørId));
        // Andre kall — skal hente fra cache, ikke kalle PDL igjen
        var resultat = personTjeneste.finnPersonIdentForAktørIdBolk(Set.of(aktørId));

        assertEquals(new PersonIdent("22222222222"), resultat.get(aktørId));
        verify(pdlKlientMock, times(1)).hentIdenterBolkResults(any(), any());
    }
}
