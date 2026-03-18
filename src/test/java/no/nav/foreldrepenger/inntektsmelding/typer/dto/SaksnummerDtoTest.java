package no.nav.foreldrepenger.inntektsmelding.typer.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class SaksnummerDtoTest {

    @Test
    void serdes_test() {
        var expectedSaksnummer = "123456789";
        var saksnummerDto = new SaksnummerDto(expectedSaksnummer);

        var ser = DefaultJsonMapper.toJson(saksnummerDto);
        var des = DefaultJsonMapper.fromJson(ser, SaksnummerDto.class);

        assertThat(ser).contains(expectedSaksnummer);
        assertThat(des).isEqualTo(saksnummerDto);
        assertThat(des.saksnr()).isEqualTo(expectedSaksnummer);
    }
}
