package no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MerkelappTest {

    @Test
    void getBeskrivelse() {
        assertThat(Merkelapp.INNTEKTSMELDING_FP.getBeskrivelse()).isEqualTo("Inntektsmelding foreldrepenger");
        assertThat(Merkelapp.INNTEKTSMELDING_SVP.getBeskrivelse()).isEqualTo("Inntektsmelding svangerskapspenger");
    }

    @Test
    void values() {
        assertThat(Merkelapp.values()).hasSize(2);
    }

    @Test
    void valueOf() {
        assertThat(Merkelapp.valueOf("INNTEKTSMELDING_FP")).isInstanceOf(Merkelapp.class).isEqualTo(Merkelapp.INNTEKTSMELDING_FP);
    }

    @Test
    void valueOf_exception() {
        assertThrows(IllegalArgumentException.class, () -> Merkelapp.valueOf("test"));
    }
}
