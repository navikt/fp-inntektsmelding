package no.nav.familie.inntektsmelding.forespørsel.rest.ekstern;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselStatusDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@ExtendWith(MockitoExtension.class)
class ForespørselEksternRestTest {
    private static final String BRREG_ORGNUMMER = "974760673";

    private ForespørselEksternRest forespørselEksternRest;
    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private Tilgang tilgang;

    @BeforeEach
    void setUp() {
        this.forespørselEksternRest = new ForespørselEksternRest(forespørselBehandlingTjeneste, tilgang);
    }

    @Test
    void skal_hente_forespørsel() {
        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var aktørIdDto = new AktørIdDto("1234567890134");
        var aktørIdEntitet = new AktørIdEntitet(aktørIdDto.id());
        var fagsakSaksnummer = ("1234567989");
        var førsteUttaksdato = LocalDate.now();
        var skjæringstidspunkt = LocalDate.now();
        var forespørselEntitet = new ForespørselEntitet(orgnummer.orgnr(),
            skjæringstidspunkt,
            aktørIdEntitet,
            Ytelsetype.FORELDREPENGER,
            fagsakSaksnummer,
            førsteUttaksdato,
            ForespørselType.BESTILT_AV_FAGSYSTEM);

        var forespørselUuid = forespørselEntitet.getUuid();

        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørselEntitet));
        var forventetForespørselDto = new ForespørselDto(forespørselUuid,
            orgnummer,
            aktørIdDto,
            førsteUttaksdato,
            skjæringstidspunkt,
            ForespørselStatusDto.UNDER_BEHANDLING,
            YtelseTypeDto.FORELDREPENGER);

        var response = forespørselEksternRest.hentForespørsel(forespørselUuid);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.getEntity()).isEqualTo(forventetForespørselDto);
        verify(forespørselBehandlingTjeneste).hentForespørsel(forespørselUuid);
    }
}
