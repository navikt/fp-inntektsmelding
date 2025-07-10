package no.nav.familie.inntektsmelding.forvaltning.rest;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.konfig.Environment;

@ExtendWith(MockitoExtension.class)
class ForespørselVtpRestTest {
    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    private ForespørselVtpRest forespørselVtpRest;

    @BeforeEach
    void setUp() {
        forespørselVtpRest = new ForespørselVtpRest(forespørselBehandlingTjeneste);
    }

    @Test
    void skal_kaste_exception_om_i_prod() {
        try (var environment = Mockito.mockStatic(Environment.class, Answers.RETURNS_DEEP_STUBS)) {
            assertThrows(RuntimeException.class, () -> forespørselVtpRest.finnForespoerselForSaksnummer(null));
        }
    }

    @Test
    void skal_ikke_kaste_exception_om_i_vtp() {
        try (var environment = Mockito.mockStatic(Environment.class, Answers.CALLS_REAL_METHODS)) {
            var vtpEnv = mock(Environment.class);
            when(vtpEnv.isLocal()).thenReturn(true);
            environment.when(Environment::current).thenReturn(vtpEnv);
            var response = forespørselVtpRest.finnForespoerselForSaksnummer(null);
            assertThat(response).isNotNull();
        }
    }

}
