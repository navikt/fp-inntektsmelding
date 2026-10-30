package no.nav.foreldrepenger.inntektsmelding.imapi.rest.forespørsel;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import jakarta.ws.rs.BadRequestException;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.rest.ForespørselRest;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.rest.OpprettEnForespørselRequest;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.rest.OpprettFlereForespørslerRequest;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.rest.OpprettForespørselRespons;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ArbeidsgiverMedResultat;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ArbeidsgiverMedStatus;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.rest.OrganisasjonsnummerMedStatusDto;
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
                LocalDate.now().plusDays(5), new OrganisasjonsnummerMedStatusDto(orgnummer, false)));

        var forventetResultat = new OpprettForespørselRespons.OrganisasjonsnummerMedStatus(orgnummer, ForespørselResultat.FORESPØRSEL_OPPRETTET);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.getEntity()).isEqualTo(forventetResultat);
        verify(forespørselBehandlingTjeneste).håndterInnkommendeForespørsel(LocalDate.now(), Ytelsetype.FORELDREPENGER,
            AktørId.fra(aktørId.id()), Arbeidsgiver.fra(BRREG_ORGNUMMER), Saksnummer.fra(fagsakSaksnummer.saksnr()), LocalDate.now().plusDays(5));
    }

    @Test
    void skal_opprette_forespørsel_for_komplett_liste() {
        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var orgnummer2 = new OrganisasjonsnummerDto(ORGNUMMER_TEST);
        var aktørId = new AktørIdDto("1234567890134");
        when(forespørselBehandlingTjeneste.håndterKomplettListeMedForespørsler(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(
                new ArbeidsgiverMedResultat(Arbeidsgiver.fra(ORGNUMMER_TEST), ForespørselResultat.FORESPØRSEL_ENDRET),
                new ArbeidsgiverMedResultat(Arbeidsgiver.fra(BRREG_ORGNUMMER), ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE)));

        var fagsakSaksnummer = new SaksnummerDto("SAK");
        var response = forespørselRest.opprettForespørslerKomplettListe(
            new OpprettFlereForespørslerRequest(aktørId, LocalDate.now(), YtelseTypeDto.FORELDREPENGER, fagsakSaksnummer,
                LocalDate.now().plusDays(5), List.of(
                    new OrganisasjonsnummerMedStatusDto(orgnummer2, true),
                    new OrganisasjonsnummerMedStatusDto(orgnummer, false)), List.of(orgnummer, orgnummer2)));

        var forventetResultat = new OpprettForespørselRespons(List.of(
            new OpprettForespørselRespons.OrganisasjonsnummerMedStatus(orgnummer2, ForespørselResultat.FORESPØRSEL_ENDRET),
            new OpprettForespørselRespons.OrganisasjonsnummerMedStatus(orgnummer, ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE)));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.getEntity()).isEqualTo(forventetResultat);

        verify(forespørselBehandlingTjeneste).håndterKomplettListeMedForespørsler(any(),
            any(),
            any(),
            eq(List.of(new ArbeidsgiverMedStatus(Arbeidsgiver.fra(ORGNUMMER_TEST), true),
                new ArbeidsgiverMedStatus(Arbeidsgiver.fra(BRREG_ORGNUMMER), false))),
            any(),
            any());
    }

    @Test
    void skal_behandle_tom_liste_som_komplett() {
        var aktørId = new AktørIdDto("1234567890134");
        var fagsakSaksnummer = new SaksnummerDto("SAK");
        when(forespørselBehandlingTjeneste.håndterKomplettListeMedForespørsler(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of());

        var response = forespørselRest.opprettForespørslerKomplettListe(
            new OpprettFlereForespørslerRequest(aktørId, LocalDate.now(), YtelseTypeDto.FORELDREPENGER, fagsakSaksnummer,
                LocalDate.now().plusDays(5), List.of(), List.of()));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.getEntity()).isEqualTo(new OpprettForespørselRespons(List.of()));
        verify(forespørselBehandlingTjeneste).håndterKomplettListeMedForespørsler(any(), any(), any(), any(), any(), any());
    }

    @Test
    void skal_avvise_ulike_organisasjonsnumre_og_duplikater_i_komplett_liste() {
        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var orgnummer2 = new OrganisasjonsnummerDto(ORGNUMMER_TEST);

        assertUgyldigeLister(List.of(orgnummer, orgnummer2), List.of(
            new OrganisasjonsnummerMedStatusDto(orgnummer, false),
            new OrganisasjonsnummerMedStatusDto(new OrganisasjonsnummerDto("123456789"), true)));
        assertUgyldigeLister(List.of(orgnummer), List.of(
            new OrganisasjonsnummerMedStatusDto(orgnummer, false),
            new OrganisasjonsnummerMedStatusDto(orgnummer2, true)));
        assertUgyldigeLister(List.of(orgnummer, orgnummer), List.of(
            new OrganisasjonsnummerMedStatusDto(orgnummer, false),
            new OrganisasjonsnummerMedStatusDto(orgnummer, true)));
        assertUgyldigeLister(List.of(orgnummer, orgnummer, orgnummer2), List.of(
            new OrganisasjonsnummerMedStatusDto(orgnummer, false),
            new OrganisasjonsnummerMedStatusDto(orgnummer2, true)));
        assertUgyldigeLister(List.of(orgnummer, orgnummer2), List.of(
            new OrganisasjonsnummerMedStatusDto(orgnummer, false),
            new OrganisasjonsnummerMedStatusDto(orgnummer2, true),
            new OrganisasjonsnummerMedStatusDto(orgnummer2, false)));
        assertUgyldigeLister(List.of(orgnummer, orgnummer2), List.of(
            new OrganisasjonsnummerMedStatusDto(orgnummer, false),
            new OrganisasjonsnummerMedStatusDto(orgnummer, true)));
        assertUgyldigeLister(List.of(orgnummer, orgnummer2), List.of(
            new OrganisasjonsnummerMedStatusDto(orgnummer, false),
            new OrganisasjonsnummerMedStatusDto(orgnummer, true),
            new OrganisasjonsnummerMedStatusDto(orgnummer2, false)));

        verifyNoInteractions(forespørselBehandlingTjeneste);
    }

    private void assertUgyldigeLister(List<OrganisasjonsnummerDto> orgnumre,
                                      List<OrganisasjonsnummerMedStatusDto> orgnumreMedStatus) {
        assertThatThrownBy(() -> forespørselRest.opprettForespørslerKomplettListe(komplettListeRequest(orgnumre, orgnumreMedStatus)))
            .isInstanceOf(BadRequestException.class);
    }

    private OpprettFlereForespørslerRequest komplettListeRequest(List<OrganisasjonsnummerDto> orgnumre,
                                                                List<OrganisasjonsnummerMedStatusDto> orgnumreMedStatus) {
        return new OpprettFlereForespørslerRequest(new AktørIdDto("1234567890134"),
            LocalDate.now(),
            YtelseTypeDto.FORELDREPENGER,
            new SaksnummerDto("SAK"),
            LocalDate.now().plusDays(5),
            orgnumreMedStatus,
            orgnumre);
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
