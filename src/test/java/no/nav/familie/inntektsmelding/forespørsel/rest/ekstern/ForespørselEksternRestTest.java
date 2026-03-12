package no.nav.familie.inntektsmelding.forespørsel.rest.ekstern;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselStatusDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

@ExtendWith(MockitoExtension.class)
class ForespørselEksternRestTest {
    private static final String BRREG_ORGNUMMER = "974760673";

    private ForespørselEksternRest forespørselEksternRest;
    @Mock
    private Tilgang tilgang;
    @Mock
    private ForespørselEksternTjeneste forespørselEksternTjeneste;

    @BeforeEach
    void setUp() {
        this.forespørselEksternRest = new ForespørselEksternRest(forespørselEksternTjeneste, tilgang);
    }

    @Test
    void skal_hente_forespørsel() {
        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var førsteUttaksdato = LocalDate.now();
        var skjæringstidspunkt = LocalDate.now();
        var forespørselUuid = UUID.randomUUID();
        var forventetForespørselDto = new ForespørselDto(forespørselUuid,
            orgnummer,
            "11111111111",
            førsteUttaksdato,
            skjæringstidspunkt,
            ForespørselStatusDto.UNDER_BEHANDLING,
            YtelseTypeDto.FORELDREPENGER,
            LocalDateTime.now());

        when(forespørselEksternTjeneste.hentForesørselDto(forespørselUuid)).thenReturn(Optional.of(forventetForespørselDto));

        var response = forespørselEksternRest.hentForespørsel(forespørselUuid);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.getEntity()).isEqualTo(forventetForespørselDto);
        verify(forespørselEksternTjeneste).hentForesørselDto(forespørselUuid);
    }
}
