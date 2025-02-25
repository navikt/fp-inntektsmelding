package no.nav.familie.inntektsmelding.integrasjoner.aareg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.ArbeidsforholdDto;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ExtendWith(MockitoExtension.class)
class AaregRestKlientTest {

    @Mock
    private RestClient restClient;

    private AaregRestKlient aaregRestKlient;

    @BeforeEach
    void setUp() {
        this.aaregRestKlient = new AaregRestKlient(restClient);
    }

    @Test
    void skal_hente_nåværende_arbeidsforhold_for_person() {
        var ident = "12345678901";

        var arbeidsforhold = new ArbeidsforholdDto(
            "123",
            1234L,
            null,
            null,
            null,
            null,
            "ordinært");

        var httpResponse = mock(HttpResponse.class);
        when(httpResponse.body()).thenReturn(DefaultJsonMapper.toJson(List.of(arbeidsforhold)));
        when(httpResponse.statusCode()).thenReturn(200);


        when(restClient.sendReturnUnhandled(any(RestRequest.class)))
            .thenReturn(httpResponse);

        var result = aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident, LocalDate.now());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(arbeidsforhold);
        assertTrue(result.getFirst().arbeidsavtaler().isEmpty());
        assertTrue(result.getFirst().permisjonPermitteringer().isEmpty());

        var requestCaptor = ArgumentCaptor.forClass(RestRequest.class);
        verify(restClient).sendReturnUnhandled(requestCaptor.capture());
    }

    @Test
    void skal_kaste_exception_ved_ugyldig_uri() {
        // Arrange
        var ident = "12345678901";

        when(restClient.sendReturnUnhandled(any(RestRequest.class)))
            .thenThrow(new IllegalArgumentException("Invalid URI"));

        // Act & Assert
        var exception = assertThrows(IllegalArgumentException.class,
            () -> aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident, LocalDate.now()));

        assertThat(exception.getMessage())
            .isEqualTo("Utviklerfeil syntax-exception for finnArbeidsforholdForArbeidstaker");
    }

    @Test
    void skal_returnere_tom_liste_ved_ingen_arbeidsforhold() {
        // Arrange
        var ident = "12345678901";

        var httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(404);


        when(restClient.sendReturnUnhandled(any(RestRequest.class)))
            .thenReturn(httpResponse);

        // Act
        var result = aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident, LocalDate.now());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void skal_returnere_tom_liste_når_aareg_returnerer_404() {
        // Arrange
        var ident = "12345678901";

        when(restClient.sendReturnUnhandled(any(RestRequest.class)))
            .thenThrow(new IntegrasjonException("FP-12345", "404 feil"));

        // Act
        var result = aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident, LocalDate.now());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void skal_kaste_exception_når_aareg_kaster_annen_integrasjonsexception() {
        // Arrange
        var ident = "12345678901";

        when(restClient.sendReturnUnhandled(any(RestRequest.class)))
            .thenThrow(new IntegrasjonException("FP-w00t", "Ukjent feil"));

        assertThrows(IntegrasjonException.class, () -> aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident, LocalDate.now()));
    }

    @Test
    void skal_bygge_korrekt_uri_for_arbeidsforhold() {
        // Arrange
        var fom = LocalDate.of(2024, 1, 1);
        var tom = LocalDate.of(2024, 3, 31);

        // Act
        var uri = aaregRestKlient.lagUriForForFinnArbeidsforholdForArbeidstaker(fom, tom);

        // Assert
        assertThat(uri.getPath()).endsWith("/arbeidsforhold");
        assertThat(uri.getQuery())
            .contains("ansettelsesperiodeFom=2024-01-01")
            .contains("ansettelsesperiodeTom=2024-03-31")
            .contains("regelverk=A_ORDNINGEN")
            .contains("historikk=true")
            .contains("sporingsinformasjon=false");
    }
}
