package no.nav.foreldrepenger.inntektsmelding.imapi.rest.forespørsel;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.ForespørselApiResponseDto;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.ArbeidsgiverInformasjonDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.YtelseType;
import no.nav.foreldrepenger.inntektsmelding.server.tilgangsstyring.Tilgang;

@ExtendWith(MockitoExtension.class)
class ForespørselApiRestTest {
    private static final String BRREG_ORGNUMMER = "974760673";

    private ForespørselApiRest forespørselApiRest;
    @Mock
    private Tilgang tilgang;
    @Mock
    private ForespørselApiTjeneste forespørselApiTjeneste;

    @BeforeEach
    void setUp() {
        this.forespørselApiRest = new ForespørselApiRest(forespørselApiTjeneste, tilgang);
    }

    @Test
    void skal_hente_forespørsel() {
        var orgnummer = new ArbeidsgiverInformasjonDto(BRREG_ORGNUMMER);
        var førsteUttaksdato = LocalDate.now();
        var skjæringstidspunkt = LocalDate.now();
        var forespørselUuid = UUID.randomUUID();
        var forventetForespørselDto = new ForespørselApiResponseDto(forespørselUuid,
            orgnummer,
            "11111111111",
            førsteUttaksdato,
            skjæringstidspunkt,
            ForespørselApiResponseDto.Status.UNDER_BEHANDLING,
            YtelseType.FORELDREPENGER,
            LocalDateTime.now());

        when(forespørselApiTjeneste.hentForesørselDto(forespørselUuid)).thenReturn(Optional.of(forventetForespørselDto));

        var response = forespørselApiRest.hentForespørsel(forespørselUuid);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.getEntity()).isEqualTo(forventetForespørselDto);
        verify(forespørselApiTjeneste).hentForesørselDto(forespørselUuid);
    }
}
