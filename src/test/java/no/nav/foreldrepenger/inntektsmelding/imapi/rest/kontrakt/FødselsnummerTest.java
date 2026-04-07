package no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FodselsnummerDtoTest {

    @Test
    void skal_returnere_fnr() {
        var fnr = new FødselsnummerDto("12345678901");
        assertThat(fnr.fnr()).isEqualTo("12345678901");
    }

    @Test
    void toString_skal_maskere_fnr() {
        var fnr = new FødselsnummerDto("12345678901");
        assertThat(fnr).hasToString("FødselsnummerDto<123456*****>");
    }

    @Test
    void toString_skal_maskere_kort_fnr() {
        var fnr = new FødselsnummerDto("1234");
        assertThat(fnr).hasToString("FødselsnummerDto<****>");
    }

    @Test
    void toString_skal_maskere_fnr_med_seks_tegn() {
        var fnr = new FødselsnummerDto("123456");
        assertThat(fnr).hasToString("FødselsnummerDto<******>");
    }

    @Test
    void toString_skal_vise_forste_seks_ved_syv_tegn() {
        var fnr = new FødselsnummerDto("1234567");
        assertThat(fnr).hasToString("FødselsnummerDto<123456*>");
    }

    @Test
    void toString_skal_håndtere_null_fnr() {
        var fnr = new FødselsnummerDto(null);
        assertThat(fnr).hasToString("FødselsnummerDto<>");
    }

    @Test
    void skal_ha_equals_basert_på_fnr() {
        var fnr1 = new FødselsnummerDto("12345678901");
        var fnr2 = new FødselsnummerDto("12345678901");
        var fnr3 = new FødselsnummerDto("99988877766");

        assertThat(fnr1).isEqualTo(fnr2)
            .isNotEqualTo(fnr3);
    }
}
