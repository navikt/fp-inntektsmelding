package no.nav.familie.inntektsmelding.forespørsel.rest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@ExtendWith(MockitoExtension.class)
class ForespørselRestTest {

    private static final String BRREG_ORGNUMMER = "974760673";
    private static final String ORGNUMMER_TEST = "450674427";

    private ForespørselRest forespørselRest;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private Tilgang tilgang;

    @BeforeEach
    void setUp() {
        this.forespørselBehandlingTjeneste = Mockito.mock(ForespørselBehandlingTjeneste.class);
        this.forespørselRest = new ForespørselRest(forespørselBehandlingTjeneste, tilgang);
    }

    @Test
    void skal_opprette_forespørsel() {
        mockForespørsel();

        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var aktørId = new AktørIdDto("1234567890134");

        var fagsakSaksnummer = new SaksnummerDto("SAK");
        var response = forespørselRest.opprettForespørsel(
            new OpprettForespørselRequest(aktørId, null, LocalDate.now(), YtelseTypeDto.FORELDREPENGER, fagsakSaksnummer, LocalDate.now().plusDays(5), List.of(orgnummer)));

        var forventetResultat = new OpprettForespørselResponsNy(List.of(new OpprettForespørselResponsNy.OrganisasjonsnummerMedStatus(orgnummer, ForespørselResultat.FORESPØRSEL_OPPRETTET)));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.getEntity()).isEqualTo(forventetResultat);
        verify(forespørselBehandlingTjeneste).håndterInnkommendeForespørsel(LocalDate.now(), Ytelsetype.FORELDREPENGER,
            new AktørIdEntitet(aktørId.id()), orgnummer, fagsakSaksnummer, LocalDate.now().plusDays(5));
    }

    @Test
    void skal_opprette_forespørsel_for_flere_organiasjonsnumre() {
        mockForespørsel();

        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var orgnummer2 = new OrganisasjonsnummerDto(ORGNUMMER_TEST);
        var aktørId = new AktørIdDto("1234567890134");

        var fagsakSaksnummer = new SaksnummerDto("SAK");
        var response = forespørselRest.opprettForespørsel(
            new OpprettForespørselRequest(aktørId, null, LocalDate.now(), YtelseTypeDto.FORELDREPENGER, fagsakSaksnummer, LocalDate.now().plusDays(5), List.of(orgnummer, orgnummer2)));

        var forventetResultat = new OpprettForespørselResponsNy(List.of(
            new OpprettForespørselResponsNy.OrganisasjonsnummerMedStatus(orgnummer, ForespørselResultat.FORESPØRSEL_OPPRETTET),
            new OpprettForespørselResponsNy.OrganisasjonsnummerMedStatus(orgnummer2, ForespørselResultat.FORESPØRSEL_OPPRETTET)));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.getEntity()).isEqualTo(forventetResultat);

        verify(forespørselBehandlingTjeneste, times(2)).håndterInnkommendeForespørsel(any(), any(), any(), any(), any(), any());
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
