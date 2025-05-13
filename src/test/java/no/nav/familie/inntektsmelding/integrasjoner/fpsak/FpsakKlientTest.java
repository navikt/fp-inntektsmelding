package no.nav.familie.inntektsmelding.integrasjoner.fpsak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
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
    void teste_kall() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        when(restClient.sendReturnOptional(any(), any())).thenReturn(Optional.of(new FpsakKlient.SakInntektsmeldingResponse(true)));
        var resultat = fpsakKlient.harSøkerSakIFagsystem(aktørId, Ytelsetype.FORELDREPENGER);

        assertThat(resultat).isTrue();
    }

    @Test
    void test_hent_info_om_sak() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var ytelse = Ytelsetype.FORELDREPENGER;
        var førsteUttaksdato = LocalDate.now();
        var skjæringstidspunkt = førsteUttaksdato.plusDays(1);

        when(restClient.sendReturnOptional(any(),
            any())).thenReturn(Optional.of(new FpsakKlient.InfoOmSakInntektsmeldingResponse(FpsakKlient.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING,
            førsteUttaksdato, skjæringstidspunkt)));
        var resultat = fpsakKlient.hentInfoOmSak(aktørId, ytelse);

        assertThat(resultat.statusInntektsmelding()).isEqualTo(FpsakKlient.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING);
        assertThat(resultat.førsteUttaksdato()).isEqualTo(førsteUttaksdato);
        assertThat(resultat.skjæringstidspunkt()).isEqualTo(skjæringstidspunkt);
    }
}
