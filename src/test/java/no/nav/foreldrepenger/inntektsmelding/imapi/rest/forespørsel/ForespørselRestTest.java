package no.nav.foreldrepenger.inntektsmelding.imapi.rest.forespørsel;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.rest.ForespørselRest;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.rest.OpprettEnForespørselRequest;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.rest.OpprettFlereForespørslerRequest;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.rest.OpprettForespørselRequest;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.rest.OpprettForespørselResponsNy;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

@ExtendWith(MockitoExtension.class)
class ForespørselRestTest {

    private static final String BRREG_ORGNUMMER = "974760673";
    private static final String ORGNUMMER_TEST = "450674427";

    private ForespørselRest forespørselRest;
    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private Tilgang tilgang;

    @BeforeEach
    void setUp() {
        this.forespørselRest = new ForespørselRest(forespørselBehandlingTjeneste, tilgang);
    }

    @Test
    void skal_opprette_forespørsel() {
        mockForespørsel();

        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var aktørId = new AktørIdDto("1234567890134");

        var fagsakSaksnummer = new SaksnummerDto("SAK");
        var response = forespørselRest.opprettEnForespørsel(
            new OpprettEnForespørselRequest(aktørId, orgnummer, LocalDate.now(), YtelseTypeDto.FORELDREPENGER, fagsakSaksnummer,
                LocalDate.now().plusDays(5)));

        var forventetResultat = new OpprettForespørselResponsNy(List.of(new OpprettForespørselResponsNy.OrganisasjonsnummerMedStatus(orgnummer, ForespørselResultat.FORESPØRSEL_OPPRETTET)));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.getEntity()).isEqualTo(forventetResultat);
        verify(forespørselBehandlingTjeneste).håndterInnkommendeForespørsel(LocalDate.now(), Ytelsetype.FORELDREPENGER,
            AktørId.fra(aktørId.id()), Arbeidsgiver.fra(BRREG_ORGNUMMER), Saksnummer.fra(fagsakSaksnummer.saksnr()), LocalDate.now().plusDays(5));
    }

    @Test
    void legacy_endepunkt_skal_fortsatt_opprette_forespørsler() {
        mockForespørsel();

        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var orgnummer2 = new OrganisasjonsnummerDto(ORGNUMMER_TEST);
        var aktørId = new AktørIdDto("1234567890134");
        var fagsakSaksnummer = new SaksnummerDto("SAK");

        var response = forespørselRest.opprettForespørsel(
            new OpprettForespørselRequest(aktørId, null, LocalDate.now(), YtelseTypeDto.FORELDREPENGER, fagsakSaksnummer,
                LocalDate.now().plusDays(5), List.of(orgnummer, orgnummer2)));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        verify(forespørselBehandlingTjeneste, times(2)).håndterInnkommendeForespørsel(any(), any(), any(), any(), any(), any());
    }

    @Test
    void legacy_endepunkt_skal_fortsatt_returnere_no_content_for_tom_liste() {
        var request = new OpprettForespørselRequest(new AktørIdDto("1234567890134"), null, LocalDate.now(),
            YtelseTypeDto.FORELDREPENGER, new SaksnummerDto("SAK"), LocalDate.now().plusDays(5), List.of());

        var response = forespørselRest.opprettForespørsel(request);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
        verifyNoInteractions(forespørselBehandlingTjeneste);
    }

    @Test
    void skal_opprette_forespørsel_for_komplett_liste() {
        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var orgnummer2 = new OrganisasjonsnummerDto(ORGNUMMER_TEST);
        var aktørId = new AktørIdDto("1234567890134");
        when(forespørselBehandlingTjeneste.håndterKomplettListeMedForespørsler(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(ForespørselResultat.FORESPØRSEL_OPPRETTET, ForespørselResultat.FORESPØRSEL_OPPRETTET));

        var fagsakSaksnummer = new SaksnummerDto("SAK");
        var response = forespørselRest.opprettFlereForespørsler(
            new OpprettFlereForespørslerRequest(aktørId, LocalDate.now(), YtelseTypeDto.FORELDREPENGER, fagsakSaksnummer,
                LocalDate.now().plusDays(5), List.of(orgnummer, orgnummer2)));

        var forventetResultat = new OpprettForespørselResponsNy(List.of(
            new OpprettForespørselResponsNy.OrganisasjonsnummerMedStatus(orgnummer, ForespørselResultat.FORESPØRSEL_OPPRETTET),
            new OpprettForespørselResponsNy.OrganisasjonsnummerMedStatus(orgnummer2, ForespørselResultat.FORESPØRSEL_OPPRETTET)));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.getEntity()).isEqualTo(forventetResultat);

        verify(forespørselBehandlingTjeneste).håndterKomplettListeMedForespørsler(any(), any(), any(), any(), any(), any());
    }

    @Test
    void skal_behandle_tom_liste_som_komplett() {
        var aktørId = new AktørIdDto("1234567890134");
        var fagsakSaksnummer = new SaksnummerDto("SAK");
        when(forespørselBehandlingTjeneste.håndterKomplettListeMedForespørsler(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of());

        var response = forespørselRest.opprettFlereForespørsler(
            new OpprettFlereForespørslerRequest(aktørId, LocalDate.now(), YtelseTypeDto.FORELDREPENGER, fagsakSaksnummer,
                LocalDate.now().plusDays(5), List.of()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.getEntity()).isEqualTo(new OpprettForespørselResponsNy(List.of()));
        verify(forespørselBehandlingTjeneste).håndterKomplettListeMedForespørsler(any(), any(), any(), any(), any(), any());
    }

    private void mockForespørsel() {
        when(forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(any(),
            any(),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(ForespørselResultat.FORESPØRSEL_OPPRETTET);
    }
}
