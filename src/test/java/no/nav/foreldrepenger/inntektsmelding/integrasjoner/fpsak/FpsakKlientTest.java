package no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;

@ExtendWith(MockitoExtension.class)
class FpsakKlientTest {
    private static final String AKTØR_ID = "1234567891234";

    @Mock
    private RestClient restClient;

    private FpsakKlient fpsakKlient;

    @BeforeEach
    void setUP() {
        fpsakKlient = new FpsakKlient(restClient);
    }

    @Test
    void test_hent_info_om_sak() {
        var aktørId = AktørId.fra(AKTØR_ID);
        var ytelse = Ytelsetype.FORELDREPENGER;
        var førsteUttaksdato = LocalDate.now();
        var skjæringstidspunkt = førsteUttaksdato.plusDays(1);

        when(restClient.sendReturnList(any(),
            any())).thenReturn(List.of(new FpsakKlient.InfoOmSakInntektsmeldingResponse(FpsakKlient.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING,
            førsteUttaksdato, skjæringstidspunkt, "12345")));
        var resultatListe = fpsakKlient.hentSaksoversiktRelevantForInntektsmeldinger(aktørId, ytelse);

        assertThat(resultatListe).hasSize(1);
        var resultat = resultatListe.getFirst();
        assertThat(resultat.statusInntektsmelding()).isEqualTo(FpsakKlient.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING);
        assertThat(resultat.førsteUttaksdato()).isEqualTo(førsteUttaksdato);
        assertThat(resultat.skjæringstidspunkt()).isEqualTo(skjæringstidspunkt);
    }

    @Test
    void test_sjekk_forespørsel_status() {
        var forespørsel = new FpsakKlient.ForespørselStatusRequest.Forespørsel("SAK123", "999999999");

        when(restClient.sendReturnList(any(), any())).thenReturn(
            List.of(new FpsakKlient.ForespørselStatusResponse("SAK123", "999999999", FpsakKlient.ForespørselStatusResponse.Vurdering.TRENGS_IKKE,
                FpsakKlient.ForespørselStatusResponse.Årsak.SAK_AVSLUTTET)));

        var resultatListe = fpsakKlient.sjekkForespørselStatus(List.of(forespørsel));

        assertThat(resultatListe).hasSize(1);
        var resultat = resultatListe.getFirst();
        assertThat(resultat.fagsakSaksnummer()).isEqualTo("SAK123");
        assertThat(resultat.orgnummer()).isEqualTo("999999999");
        assertThat(resultat.vurdering()).isEqualTo(FpsakKlient.ForespørselStatusResponse.Vurdering.TRENGS_IKKE);
        assertThat(resultat.årsak()).isEqualTo(FpsakKlient.ForespørselStatusResponse.Årsak.SAK_AVSLUTTET);
    }

    @Test
    void test_sjekk_forespørsel_status_skal_feile_ved_for_stor_batch() {
        var forForMange = new ArrayList<FpsakKlient.ForespørselStatusRequest.Forespørsel>();
        for (var i = 0; i < FpsakKlient.MAKS_ANTALL_FORESPØRSLER_PER_KALL + 1; i++) {
            forForMange.add(new FpsakKlient.ForespørselStatusRequest.Forespørsel("SAK" + i, "999999999"));
        }

        assertThatThrownBy(() -> fpsakKlient.sjekkForespørselStatus(forForMange)).isInstanceOf(IllegalArgumentException.class);
    }
}
