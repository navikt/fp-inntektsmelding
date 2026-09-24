package no.nav.foreldrepenger.inntektsmelding.imapi.rest.inntekt;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.imapi.inntekt.InntektResponse;
import no.nav.foreldrepenger.inntektsmelding.server.tilgangsstyring.Tilgang;

@ExtendWith(MockitoExtension.class)
class InntektApiRestTest {
    private InntektApiRest inntektApiRest;
    @Mock
    private Tilgang tilgang;
    @Mock
    private InntektApiTjeneste inntektApiTjeneste;

    @BeforeEach
    void setUp() {
        this.inntektApiRest = new InntektApiRest(inntektApiTjeneste, tilgang);
    }

    @Test
    void skal_hente_inntekt() {
        var forespørselUuid = UUID.randomUUID();
        var forventetInntektDto = new InntektResponse(Map.of(YearMonth.of(2025, 3), BigDecimal.valueOf(30000)), BigDecimal.valueOf(30000));
        when(inntektApiTjeneste.hentInntektDto(forespørselUuid)).thenReturn(Optional.of(forventetInntektDto));

        var response = inntektApiRest.hentInntekt(forespørselUuid);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.getEntity()).isEqualTo(forventetInntektDto);
        verify(tilgang).sjekkErSystembruker();
    }

    @Test
    void skal_returnere_404_når_forespørsel_ikke_finnes() {
        var forespørselUuid = UUID.randomUUID();
        when(inntektApiTjeneste.hentInntektDto(forespørselUuid)).thenReturn(Optional.empty());

        var response = inntektApiRest.hentInntekt(forespørselUuid);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
    }
}
