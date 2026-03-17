package no.nav.familie.inntektsmelding.overstyring;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

class InntektsmeldingOverstyringMapperTest {

    @Test
    void skal_teste_overstyring_mapping() {
        // Arrange
        var stp = LocalDate.now();
        var foventedeRefusjonsendringer = List.of(new SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto(stp.plusDays(10),
                BigDecimal.valueOf(4000)),
            new SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto(stp.plusDays(15), BigDecimal.ZERO));
        var request = new SendOverstyrtInntektsmeldingRequestDto(new AktørIdDto("9999999999999"),
            YtelseTypeDto.FORELDREPENGER,
            new OrganisasjonsnummerDto("999999999"),
            stp,
            BigDecimal.valueOf(5000),
            BigDecimal.valueOf(5000),
            foventedeRefusjonsendringer,
            Collections.emptyList(),
            "Truls Test",
            new SaksnummerDto("1234"));

        // Act
        var entitet = InntektsmeldingOverstyringMapper.mapTilEntitet(request);

        // Assert
        assertThat(entitet).isNotNull();
        assertThat(entitet.getRefusjonsendringer()).hasSize(1);
        assertThat(entitet.getRefusjonsendringer().getFirst().getFom()).isEqualTo(stp.plusDays(10));
        assertThat(entitet.getRefusjonsendringer().getFirst().getRefusjonPrMnd()).isEqualByComparingTo(BigDecimal.valueOf(4000));
        // Opphørsdato skal være siste dag med refusjon, ikke første dag uten
        assertThat(entitet.getOpphørsdatoRefusjon()).isEqualTo(stp.plusDays(14));
        assertThat(entitet.getMånedRefusjon()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

}
