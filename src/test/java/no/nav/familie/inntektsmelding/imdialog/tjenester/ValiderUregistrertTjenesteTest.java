package no.nav.familie.inntektsmelding.imdialog.tjenester;

import no.nav.familie.inntektsmelding.integrasjoner.fpsak.FpsakKlient;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.konfig.Tid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValiderUregistrertTjenesteTest {
    private static final LocalDate FØRSTE_UTTAKSDATO = LocalDate.now();
    private static final AktørIdEntitet AKTØR_ID_ENTITET = new AktørIdEntitet("1234567891234");
    private static final PersonInfo PERSON_INFO = new PersonInfo("Navn",
        null,
        "Navnesen",
        new PersonIdent("01019100000"),
        AKTØR_ID_ENTITET,
        LocalDate.of(1991, 1, 1).minusYears(30),
        null,
        null);
    private static final String ORGANISASJONSNUMMER = "123456789";
    private ValiderUregistrertTjeneste validerUregistrertTjeneste;
    @Mock
    private GrunnlagDtoTjeneste grunnlagDtoTjeneste;

    @BeforeEach
    public void setup() {
        validerUregistrertTjeneste = new ValiderUregistrertTjeneste(grunnlagDtoTjeneste);
    }

    @Test
    void uregistrert_skal_ikke_opprettet_finnes_ikke_sak() {
        var infoOmSak = new FpsakKlient.InfoOmSakInntektsmeldingResponse(FpsakKlient.StatusSakInntektsmelding.INGEN_BEHANDLING,
            Tid.TIDENES_ENDE,
            Tid.TIDENES_ENDE);

        var ex = assertThrows(FunksjonellException.class, () -> validerUregistrertTjeneste.validerOmUregistrertKanOpprettes(infoOmSak,
            FØRSTE_UTTAKSDATO,
            Ytelsetype.FORELDREPENGER,
            PERSON_INFO,
            ORGANISASJONSNUMMER));

        assertThat(ex.getMessage()).isEqualTo(
            "INGEN_SAK_FUNNET:Du kan ikke sende inn inntektsmelding på FORELDREPENGER for denne personen med aktør id AktørIdEntitet{aktørId='*********1234'}");
    }

    @Test
    void uregistrert_skal_ikke_opprettet_søkt_for_tidlig_aksjonspunkt() {
        var infoOmSak = new FpsakKlient.InfoOmSakInntektsmeldingResponse(FpsakKlient.StatusSakInntektsmelding.SØKT_FOR_TIDLIG,
            FØRSTE_UTTAKSDATO,
            FØRSTE_UTTAKSDATO);

        var ex = assertThrows(FunksjonellException.class, () -> validerUregistrertTjeneste.validerOmUregistrertKanOpprettes(infoOmSak,
            FØRSTE_UTTAKSDATO,
            Ytelsetype.FORELDREPENGER,
            PERSON_INFO,
            ORGANISASJONSNUMMER));

        assertThat(ex.getMessage()).isEqualTo(
            "SENDT_FOR_TIDLIG:Du kan ikke sende inn inntektsmelding før fire uker før personen med aktør id AktørIdEntitet{aktørId='*********1234'} starter foreldrepenger");
    }

    @Test
    void uregistrert_skal_ikke_opprettet_søkt_for_tidlig_beregnet_fra_første_uttaksdato() {
        var førsteUttaksdatoForTidlig = FØRSTE_UTTAKSDATO.plusMonths(4).plusDays(1);
        var infoOmSak = new FpsakKlient.InfoOmSakInntektsmeldingResponse(FpsakKlient.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING,
            førsteUttaksdatoForTidlig,
            FØRSTE_UTTAKSDATO.plusMonths(5).minusDays(2));

        var ex = assertThrows(FunksjonellException.class, () -> validerUregistrertTjeneste.validerOmUregistrertKanOpprettes(infoOmSak,
            førsteUttaksdatoForTidlig,
            Ytelsetype.FORELDREPENGER,
            PERSON_INFO,
            ORGANISASJONSNUMMER));

        assertThat(ex.getMessage()).isEqualTo(
            "SENDT_FOR_TIDLIG:Du kan ikke sende inn inntektsmelding før fire uker før personen med aktør id AktørIdEntitet{aktørId='*********1234'} starter foreldrepenger");

    }

    @Test
    void uregistrert_skal_ikke_opprettet_orgnummer_har_registrering_i_aa_reg() {
        var infoOmSak = new FpsakKlient.InfoOmSakInntektsmeldingResponse(FpsakKlient.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING,
            FØRSTE_UTTAKSDATO,
            FØRSTE_UTTAKSDATO.minusDays(2));

        when(grunnlagDtoTjeneste.finnesOrgnummerIAaregPåPerson(any(), any(),
            any())).thenReturn(true);

        var ex = assertThrows(FunksjonellException.class, () -> validerUregistrertTjeneste.validerOmUregistrertKanOpprettes(infoOmSak,
            FØRSTE_UTTAKSDATO,
            Ytelsetype.FORELDREPENGER,
            PERSON_INFO,
            ORGANISASJONSNUMMER));

        assertThat(ex.getMessage()).isEqualTo(
            "ORGNR_FINNES_I_AAREG:Det finnes rapportering i aa-registeret på organisasjonsnummeret. Nav vil be om inntektsmelding når vi trenger det");
    }

    @Test
    void uregistrert_kan_opprettes() {
        var infoOmSak = new FpsakKlient.InfoOmSakInntektsmeldingResponse(FpsakKlient.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING,
            FØRSTE_UTTAKSDATO,
            FØRSTE_UTTAKSDATO.minusDays(2));

        when(grunnlagDtoTjeneste.finnesOrgnummerIAaregPåPerson(any(), any(),
            any())).thenReturn(false);

        assertDoesNotThrow(() -> validerUregistrertTjeneste.validerOmUregistrertKanOpprettes(
            infoOmSak,
            FØRSTE_UTTAKSDATO,
            Ytelsetype.FORELDREPENGER,
            PERSON_INFO,
            ORGANISASJONSNUMMER
        ));
    }
}
