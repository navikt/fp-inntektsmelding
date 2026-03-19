package no.nav.foreldrepenger.inntektsmelding.typer.domene;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArbeidsgiverTest {

    @Test
    void skal_opprette_med_konstruktor() {
        var arbeidsgiver = new Arbeidsgiver("974760673");
        assertThat(arbeidsgiver.orgnr()).isEqualTo("974760673");
    }

    @Test
    void skal_opprette_med_fra() {
        var arbeidsgiver = Arbeidsgiver.fra("974760673");
        assertThat(arbeidsgiver.orgnr()).isEqualTo("974760673");
    }

    @Test
    void skal_maskere_orgnr_i_toString() {
        var arbeidsgiver = Arbeidsgiver.fra("974760673");
        assertThat(arbeidsgiver).hasToString("Arbeidsgiver<*****0673>");
    }

    @Test
    void skal_maskere_kort_orgnr_helt() {
        var arbeidsgiver = Arbeidsgiver.fra("1234");
        assertThat(arbeidsgiver).hasToString("Arbeidsgiver<****>");
    }

    @Test
    void skal_maskere_veldig_kort_orgnr() {
        var arbeidsgiver = Arbeidsgiver.fra("ab");
        assertThat(arbeidsgiver).hasToString("Arbeidsgiver<**>");
    }

    @Test
    void skal_håndtere_null_orgnr_i_toString() {
        var arbeidsgiver = new Arbeidsgiver(null);
        assertThat(arbeidsgiver).hasToString("Arbeidsgiver<>");
    }

    @Test
    void skal_være_lik_ved_samme_orgnr() {
        var a = Arbeidsgiver.fra("974760673");
        var b = Arbeidsgiver.fra("974760673");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void skal_ikke_være_lik_ved_ulikt_orgnr() {
        var a = Arbeidsgiver.fra("974760673");
        var b = Arbeidsgiver.fra("999999999");
        assertThat(a).isNotEqualTo(b);
    }
}

