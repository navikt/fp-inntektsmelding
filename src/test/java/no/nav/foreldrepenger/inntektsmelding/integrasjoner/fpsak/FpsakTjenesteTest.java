package no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

@ExtendWith(MockitoExtension.class)
class FpsakTjenesteTest {

    @Mock
    private FpsakKlient fpsakKlient;

    private FpsakTjeneste fpsakTjeneste;

    @BeforeEach
    void setUp() {
        fpsakTjeneste = new FpsakTjeneste(fpsakKlient);
    }

    @Test
    void skal_mappe_første_saksoversikt_til_fagsak() {
        var aktørId = AktørId.fra("1234567891234");
        var førsteUttaksdato = LocalDate.now();
        var skjæringstidspunkt = førsteUttaksdato.plusDays(1);
        when(fpsakKlient.hentSaksoversiktRelevantForInntektsmeldinger(any(), any())).thenReturn(List.of(
            new FpsakKlient.InfoOmSakInntektsmeldingResponse(FpsakKlient.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING,
                førsteUttaksdato,
                skjæringstidspunkt,
                "12345")));

        var resultat = fpsakTjeneste.henterInfoOmSakIFagsystem(aktørId, Ytelsetype.FORELDREPENGER);

        assertThat(resultat).containsExactly(new FpsakFagsak(FpsakFagsak.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING,
            førsteUttaksdato,
            skjæringstidspunkt,
            new Saksnummer("12345")));
    }
}
