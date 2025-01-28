package no.nav.familie.inntektsmelding.integrasjoner.joark;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FagsystemTest {

    @Test
    void testRiktigOffisjelKode() {
        assertThat(Fagsystem.FPSAK.getOffisiellKode()).isEqualTo("FS36");
    }
}
