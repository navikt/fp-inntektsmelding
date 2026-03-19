package no.nav.foreldrepenger.inntektsmelding.integrasjoner.person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AktørIdTest {

    private static final String GYLDIG_AKTØR_ID = "1234567890123";

    @Test
    void skal_opprette_aktørId_med_gyldig_13_sifret_id() {
        var aktørId = AktørId.fra(GYLDIG_AKTØR_ID);

        assertThat(aktørId.getAktørId()).isEqualTo(GYLDIG_AKTØR_ID);
    }

    @Test
    void skal_kaste_NullPointerException_når_aktørId_er_null() {
        assertThatNullPointerException()
            .isThrownBy(() -> AktørId.fra(null))
            .withMessageContaining("aktørId");
    }

    @Test
    void skal_kaste_IllegalArgumentException_for_tom_streng() {
        assertThatThrownBy(() -> AktørId.fra(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ugyldig aktørId");
    }

    @Test
    void skal_kaste_IllegalArgumentException_for_for_kort_id() {
        assertThatThrownBy(() -> AktørId.fra("123456789012"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ugyldig aktørId");
    }

    @Test
    void skal_kaste_IllegalArgumentException_for_bokstaver() {
        assertThatThrownBy(() -> AktørId.fra("123456789012a"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ugyldig aktørId");
    }

    @Test
    void skal_kaste_IllegalArgumentException_for_spesialtegn() {
        assertThatThrownBy(() -> AktørId.fra("123456789012!"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ugyldig aktørId");
    }

    @Test
    void skal_maskere_aktørId_i_toString() {
        var aktørId = AktørId.fra(GYLDIG_AKTØR_ID);

        var resultat = aktørId.toString();

        assertThat(resultat)
            .contains("*********0123")
            .doesNotContain(GYLDIG_AKTØR_ID);
    }

    @Test
    void skal_være_lik_når_samme_aktørId() {
        var aktørId1 = AktørId.fra(GYLDIG_AKTØR_ID);
        var aktørId2 = AktørId.fra(GYLDIG_AKTØR_ID);

        assertThat(aktørId1)
            .isEqualTo(aktørId2)
            .hasSameHashCodeAs(aktørId2);
    }

    @Test
    void skal_ikke_være_lik_når_ulik_aktørId() {
        var aktørId1 = AktørId.fra("1234567890123");
        var aktørId2 = AktørId.fra("9876543210123");

        assertThat(aktørId1).isNotEqualTo(aktørId2);
    }

    @Test
    void skal_ikke_være_lik_null() {
        var aktørId = AktørId.fra(GYLDIG_AKTØR_ID);

        assertThat(aktørId).isNotNull();
    }
}

