package no.nav.foreldrepenger.inntektsmelding.server.tilgangsstyring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.pip.AltinnTilgangTjeneste;
import no.nav.foreldrepenger.inntektsmelding.pip.PipTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@ExtendWith(MockitoExtension.class)
class TilgangTjenesteTest {

    private Tilgang tilgangTjeneste;

    @Mock
    private PipTjeneste pipTjeneste;

    @Mock
    private AltinnTilgangTjeneste altinnTilgangTjeneste;

    @BeforeEach
    void setUp() {
        tilgangTjeneste = new TilgangTjeneste(pipTjeneste, altinnTilgangTjeneste);
    }

    @AfterEach
    void fjernKontekst() {
        KontekstHolder.fjernKontekst();
    }

    @Test
    void test_borgen_inisjert_kall_uten_request_kontekts_nok() {
        KontekstHolder.setKontekst(RequestKontekst.ikkeAutentisertRequest("testConsument"));
        var forespørselUuid = UUID.randomUUID();
        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid));
        assertThat(ex.getMessage()).contains("Kun borger kall støttes.");
        verifyNoInteractions(pipTjeneste, altinnTilgangTjeneste);
    }

    @Test
    void test_borgen_inisjert_kall_uten_riktig_token_type_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.InternBruker));
        var forespørselUuid = UUID.randomUUID();
        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid));
        assertThat(ex.getMessage()).contains("Kun borger kall støttes.");
        verifyNoInteractions(pipTjeneste, altinnTilgangTjeneste);
    }

    @Test
    void test_borgen_inisjert_kall_mangler_informasjon_om_bedrift_fra_pip_forespørsel_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var forespørselUuid = UUID.randomUUID();
        var ex = assertThrows(ManglerTilgangException.class,
            () -> {
                tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid);
            });
        assertThat(ex.getMessage()).contains("Mangler informasjon om bedrift.");

        verify(pipTjeneste).hentArbeidsgiverFor(forespørselUuid);

        verifyNoInteractions(altinnTilgangTjeneste);
    }

    @Test
    void test_borgen_inisjert_kall_mangler_tilgang_til_tjenesten_fra_pip_forespørsel_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var forespørselUuid = UUID.randomUUID();
        var fakeOrgNr = "123456789";
        when(pipTjeneste.hentArbeidsgiverFor(forespørselUuid)).thenReturn(Arbeidsgiver.fra(fakeOrgNr));
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(fakeOrgNr)).thenReturn(true);

        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid));
        assertThat(ex.getMessage()).contains("Bruker mangler tilgang til bedriften i Altinn.");

        verify(pipTjeneste).hentArbeidsgiverFor(forespørselUuid);
        verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(fakeOrgNr);

    }

    @Test
    void test_borgen_inisjert_kall_fra_pip_forespørsel_tilgang_ok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var forespørselUuid = UUID.randomUUID();
        var fakeOrgNr = "123456789";
        when(pipTjeneste.hentArbeidsgiverFor(forespørselUuid)).thenReturn(Arbeidsgiver.fra(fakeOrgNr));
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(fakeOrgNr)).thenReturn(false);

        assertDoesNotThrow(() -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid));

        verify(pipTjeneste).hentArbeidsgiverFor(forespørselUuid);
        verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(fakeOrgNr);
    }

    @Test
    void test_borger_initiert_kall_fra_organisasjon_ikke_ok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var fakeOrgNr = "123456789";

        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(fakeOrgNr)).thenReturn(true);
        var arbeidsgiver = Arbeidsgiver.fra(fakeOrgNr);
        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(arbeidsgiver));
        assertThat(ex.getMessage()).contains("Bruker mangler tilgang til bedriften i Altinn.");

        verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(fakeOrgNr);
    }

    @Test
    void test_borger_initiert_kall_fra_organisasjon_ok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var okOrgNr = "123456789";

        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(okOrgNr)).thenReturn(false);

        assertDoesNotThrow(() -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(Arbeidsgiver.fra(okOrgNr)));

        verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(okOrgNr);
    }


    @Test
    void test_borgen_inisjert_kall_mangler_informasjon_om_bedrift_fra_pip_inntektsmelding_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var inntektsmeldingId = 1L;
        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(inntektsmeldingId));
        assertThat(ex.getMessage()).contains("Mangler informasjon om bedrift.");

        verify(pipTjeneste).hentArbeidsgiverFor(inntektsmeldingId);
        verifyNoInteractions(altinnTilgangTjeneste);
    }

    @Test
    void test_borgen_inisjert_kall_mangler_tilgang_til_tjenesten_fra_pip_inntektsmelding_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var inntektsmeldingId = 1L;
        var fakeOrgNr = "123456789";
        when(pipTjeneste.hentArbeidsgiverFor(inntektsmeldingId)).thenReturn(Arbeidsgiver.fra(fakeOrgNr));
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(fakeOrgNr)).thenReturn(true);

        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(inntektsmeldingId));
        assertThat(ex.getMessage()).contains("Bruker mangler tilgang til bedriften i Altinn.");

        verify(pipTjeneste).hentArbeidsgiverFor(inntektsmeldingId);
        verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(fakeOrgNr);
    }

    @Test
    void test_borgen_inisjert_kall_fra_pip_inntektsmelding_tilgang_ok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var inntektsmeldingId = 1L;
        var fakeOrgNr = "123456789";
        when(pipTjeneste.hentArbeidsgiverFor(inntektsmeldingId)).thenReturn(Arbeidsgiver.fra(fakeOrgNr));
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(fakeOrgNr)).thenReturn(false);

        assertDoesNotThrow(() -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(inntektsmeldingId));

        verify(pipTjeneste).hentArbeidsgiverFor(inntektsmeldingId);
        verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(fakeOrgNr);
    }


    @Test
    void test_sjekk_om_ansatt_har_rollen_drift_ikke_saksbehandler_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var ex = assertThrows(ManglerTilgangException.class, () -> tilgangTjeneste.sjekkAtAnsattHarRollenDrift());
        assertThat(ex.getMessage()).contains("Ansatt mangler en rolle.");
    }

    @Test
    void test_sjekk_om_ansatt_har_rollen_drift_mangler_rollen_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.InternBruker));
        var ex = assertThrows(ManglerTilgangException.class, () -> tilgangTjeneste.sjekkAtAnsattHarRollenDrift());
        assertThat(ex.getMessage()).contains("Ansatt mangler en rolle.");
    }

    @Test
    void test_sjekk_om_ansatt_har_rollen_drift_feil_rolle_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.InternBruker, Set.of(AnsattGruppe.SAKSBEHANDLER, AnsattGruppe.VEILEDER)));
        var ex = assertThrows(ManglerTilgangException.class, () -> tilgangTjeneste.sjekkAtAnsattHarRollenDrift());
        assertThat(ex.getMessage()).contains("Ansatt mangler en rolle.");
    }

    @Test
    void test_sjekk_om_ansatt_har_rollen_drift_ok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.InternBruker, Set.of(AnsattGruppe.DRIFT)));
        assertDoesNotThrow(() -> tilgangTjeneste.sjekkAtAnsattHarRollenDrift());
    }

    @Test
    void test_sjekk_om_systembruker_kall_nok_pga_internbruker() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.InternBruker));
        var ex = assertThrows(ManglerTilgangException.class, () -> tilgangTjeneste.sjekkErSystembruker());
        assertThat(ex.getMessage()).contains("Kun systemkall støttes.");
    }

    @Test
    void test_sjekk_om_systembruker_kall_nok_pga_eksternbruker() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var ex = assertThrows(ManglerTilgangException.class, () -> tilgangTjeneste.sjekkErSystembruker());
        assertThat(ex.getMessage()).contains("Kun systemkall støttes.");
    }

    @Test
    void test_sjekk_om_systembruker_kall_ok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.Systemressurs));
        assertDoesNotThrow(() -> tilgangTjeneste.sjekkErSystembruker());
    }

    private Kontekst fakeRequestKontekts(IdentType identType) {
        return fakeRequestKontekts(identType, Set.of());
    }

    private Kontekst fakeRequestKontekts(IdentType identType, Set<AnsattGruppe> groups) {
        return RequestKontekst.forRequest("brukerUid", "brukerUid", identType, fakeOidcToken(OpenIDProvider.TOKENX), UUID.randomUUID(), groups);
    }

    private OpenIDToken fakeOidcToken(OpenIDProvider provider) {
        return new OpenIDToken(provider, new TokenString("fakeTokenString"));
    }
}
