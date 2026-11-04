package no.nav.foreldrepenger.inntektsmelding.imdialog.rest.aginitiert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.InntektsmeldingDialogDto;
import no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester.GrunnlagDtoTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakFagsak;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.foreldrepenger.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselStatusDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.exception.ManglerTilgangException;

@ExtendWith(MockitoExtension.class)
class ArbeidsgiverinitiertDialogRestTest {

    private static final PersonIdent IDENT = new PersonIdent("12345678910");
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.fra("123456789");
    private static final LocalDate DATO = LocalDate.now();

    @Mock
    private GrunnlagDtoTjeneste grunnlagDtoTjeneste;
    @Mock
    private FpsakTjeneste fpsakTjeneste;
    @Mock
    private Tilgang tilgang;

    private ArbeidsgiverinitiertDialogRest rest;

    @BeforeEach
    void setUp() {
        rest = new ArbeidsgiverinitiertDialogRest(grunnlagDtoTjeneste, fpsakTjeneste, tilgang);
    }

    @Test
    void skal_avvise_nyansatt_uten_tilgang_før_dataoppslag() {
        var avslag = new ManglerTilgangException("IM-00403", "Mangler tilgang til bedriften i Altinn.");
        doThrow(avslag).when(tilgang).sjekkAtArbeidsgiverHarTilgangTilBedrift(ARBEIDSGIVER);

        assertThatThrownBy(() -> rest.hentOpplysninger(nyansattRequest())).isSameAs(avslag);

        var rekkefølge = inOrder(tilgang);
        rekkefølge.verify(tilgang).sjekkAtArbeidsgiverHarTilgangTilBedrift(ARBEIDSGIVER);
        verifyNoMoreInteractions(tilgang);
        verifyNoInteractions(grunnlagDtoTjeneste, fpsakTjeneste);
    }

    @Test
    void skal_avvise_uregistrert_uten_tilgang_før_dataoppslag() {
        var avslag = new ManglerTilgangException("IM-00403", "Mangler tilgang til bedriften i Altinn.");
        doThrow(avslag).when(tilgang).sjekkAtArbeidsgiverHarTilgangTilBedrift(ARBEIDSGIVER);

        assertThatThrownBy(() -> rest.hentOpplysningerUregistrert(uregistrertRequest())).isSameAs(avslag);

        var rekkefølge = inOrder(tilgang);
        rekkefølge.verify(tilgang).sjekkAtArbeidsgiverHarTilgangTilBedrift(ARBEIDSGIVER);
        verifyNoMoreInteractions(tilgang);
        verifyNoInteractions(grunnlagDtoTjeneste, fpsakTjeneste);
    }

    @Test
    void skal_kontrollere_orgnr_før_nyansatt_oppslag_og_returnere_dialog() {
        var dialog = dialogDto();
        when(grunnlagDtoTjeneste.lagArbeidsgiverinitiertNyansattDialogDto(IDENT, Ytelsetype.FORELDREPENGER, DATO, ARBEIDSGIVER))
            .thenReturn(dialog);

        var response = rest.hentOpplysninger(nyansattRequest());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isSameAs(dialog);
        var rekkefølge = inOrder(tilgang, grunnlagDtoTjeneste);
        rekkefølge.verify(tilgang).sjekkAtArbeidsgiverHarTilgangTilBedrift(ARBEIDSGIVER);
        rekkefølge.verify(grunnlagDtoTjeneste)
            .lagArbeidsgiverinitiertNyansattDialogDto(IDENT, Ytelsetype.FORELDREPENGER, DATO, ARBEIDSGIVER);
        verifyNoMoreInteractions(tilgang, grunnlagDtoTjeneste);
        verifyNoInteractions(fpsakTjeneste);
    }

    @Test
    void skal_kontrollere_orgnr_før_uregistrert_oppslag_og_returnere_dialog() {
        var aktørId = AktørId.fra("1000000000000");
        var personInfo = new PersonInfo("Test", null, "Person", IDENT, aktørId, null, null, PersonInfo.Kjønn.UKJENT);
        var sak = new FpsakFagsak(FpsakFagsak.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING, DATO, DATO, Saksnummer.fra("123456"));
        var dialog = dialogDto();
        when(grunnlagDtoTjeneste.finnPersoninfo(IDENT, Ytelsetype.FORELDREPENGER)).thenReturn(personInfo);
        when(fpsakTjeneste.henterInfoOmSakIFagsystem(aktørId, Ytelsetype.FORELDREPENGER)).thenReturn(List.of(sak));
        when(grunnlagDtoTjeneste.lagArbeidsgiverinitiertUregistrertDialogDto(IDENT, Ytelsetype.FORELDREPENGER, DATO, ARBEIDSGIVER, DATO))
            .thenReturn(dialog);

        var response = rest.hentOpplysningerUregistrert(uregistrertRequest());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isSameAs(dialog);
        var rekkefølge = inOrder(tilgang, grunnlagDtoTjeneste, fpsakTjeneste);
        rekkefølge.verify(tilgang).sjekkAtArbeidsgiverHarTilgangTilBedrift(ARBEIDSGIVER);
        rekkefølge.verify(grunnlagDtoTjeneste).finnPersoninfo(IDENT, Ytelsetype.FORELDREPENGER);
        rekkefølge.verify(fpsakTjeneste).henterInfoOmSakIFagsystem(aktørId, Ytelsetype.FORELDREPENGER);
        rekkefølge.verify(grunnlagDtoTjeneste)
            .lagArbeidsgiverinitiertUregistrertDialogDto(IDENT, Ytelsetype.FORELDREPENGER, DATO, ARBEIDSGIVER, DATO);
        verifyNoMoreInteractions(tilgang, grunnlagDtoTjeneste, fpsakTjeneste);
    }

    private static OpplysningerRequestDto nyansattRequest() {
        return new OpplysningerRequestDto(IDENT, Ytelsetype.FORELDREPENGER, DATO, new OrganisasjonsnummerDto(ARBEIDSGIVER.orgnr()));
    }

    private static OpplysningerUregistrertRequestDto uregistrertRequest() {
        return new OpplysningerUregistrertRequestDto(IDENT, Ytelsetype.FORELDREPENGER, new OrganisasjonsnummerDto(ARBEIDSGIVER.orgnr()));
    }

    private static InntektsmeldingDialogDto dialogDto() {
        return new InntektsmeldingDialogDto(
            new InntektsmeldingDialogDto.PersonInfoResponseDto("Test", "", "Person", IDENT.getIdent(), "1000000000000"),
            new InntektsmeldingDialogDto.OrganisasjonInfoResponseDto("Testbedrift", ARBEIDSGIVER.orgnr()),
            new InntektsmeldingDialogDto.InnsenderDto("Test", null, "Innsender", null),
            new InntektsmeldingDialogDto.InntektsopplysningerDto(null, List.of()),
            DATO, YtelseTypeDto.FORELDREPENGER, null, ForespørselStatusDto.UNDER_BEHANDLING, DATO, List.of());
    }
}
