package no.nav.foreldrepenger.inntektsmelding.typer.domene;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SaksnummerTest {

    @Test
    void skal_opprette_med_konstruktor() {
        var saksnummer = new Saksnummer("FAGSAK_123");
        assertThat(saksnummer.saksnummer()).isEqualTo("FAGSAK_123");
    }

    @Test
    void skal_opprette_med_fra() {
        var saksnummer = Saksnummer.fra("FAGSAK_123");
        assertThat(saksnummer.saksnummer()).isEqualTo("FAGSAK_123");
    }

    @Test
    void skal_vise_saksnummer_i_toString() {
        var saksnummer = Saksnummer.fra("FAGSAK_123");
        assertThat(saksnummer).hasToString("Saksnummer[saksnummer=FAGSAK_123]");
    }

    @Test
    void skal_være_lik_ved_samme_saksnummer() {
        var a = Saksnummer.fra("SAK_1");
        var b = Saksnummer.fra("SAK_1");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void skal_ikke_være_lik_ved_ulikt_saksnummer() {
        var a = Saksnummer.fra("SAK_1");
        var b = Saksnummer.fra("SAK_2");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void skal_ikke_være_lik_null() {
        var a = Saksnummer.fra("SAK_1");
        assertThat(a).isNotNull();
    }

    @Test
    void skal_være_lik_seg_selv() {
        var a = Saksnummer.fra("SAK_1");
        var b = a;
        assertThat(a).isEqualTo(b);
    }

    @Test
    void skal_håndtere_null_saksnummer_i_equals() {
        var a = new Saksnummer(null);
        var b = new Saksnummer(null);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void skal_ha_ulik_hashcode_for_ulike_saksnumre() {
        var a = Saksnummer.fra("SAK_1");
        var b = Saksnummer.fra("SAK_2");
        assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
    }
}

