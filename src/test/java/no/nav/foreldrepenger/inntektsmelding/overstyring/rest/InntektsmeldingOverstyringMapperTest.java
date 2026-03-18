package no.nav.foreldrepenger.inntektsmelding.overstyring.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

class InntektsmeldingOverstyringMapperTest {

    // ---- Tests for mapTilDto (SendOverstyrtInntektsmeldingRequestDto -> InntektsmeldingDto) ----

    @Test
    void skal_teste_dto_mapping_med_refusjonsendring_og_opphør() {
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
        var dto = InntektsmeldingOverstyringMapper.mapTilDto(request);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getAktørId().getAktørId()).isEqualTo("9999999999999");
        assertThat(dto.getArbeidsgiver().orgnr()).isEqualTo("999999999");
        assertThat(dto.getMånedInntekt()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(dto.getMånedRefusjon()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(dto.getStartdato()).isEqualTo(stp);
        assertThat(dto.getYtelse()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(dto.getKildesystem()).isEqualTo(Kildesystem.FPSAK);
        assertThat(dto.getOpprettetAv()).isEqualTo("Truls Test");
        assertThat(dto.getSøkteRefusjonsperioder()).hasSize(1);
        assertThat(dto.getSøkteRefusjonsperioder().getFirst().fom()).isEqualTo(stp.plusDays(10));
        assertThat(dto.getSøkteRefusjonsperioder().getFirst().beløp()).isEqualByComparingTo(BigDecimal.valueOf(4000));
        // Opphørsdato skal være siste dag med refusjon, ikke første dag uten
        assertThat(dto.getOpphørsdatoRefusjon()).isEqualTo(stp.plusDays(14));
        assertThat(dto.getEndringAvInntektÅrsaker()).isEmpty();
        assertThat(dto.getBortfaltNaturalytelsePerioder()).isEmpty();
    }

    @Test
    void skal_teste_dto_mapping_uten_refusjonsendringer() {
        // Arrange
        var stp = LocalDate.now();
        var orgnr = "999999999";
        var request = new SendOverstyrtInntektsmeldingRequestDto(new AktørIdDto("9999999999999"),
            YtelseTypeDto.FORELDREPENGER,
            new OrganisasjonsnummerDto(orgnr),
            stp,
            BigDecimal.valueOf(5000),
            BigDecimal.valueOf(5000),
            Collections.emptyList(),
            Collections.emptyList(),
            "Truls Test",
            new SaksnummerDto("1234"));

        // Act
        var dto = InntektsmeldingOverstyringMapper.mapTilDto(request);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getSøkteRefusjonsperioder()).isEmpty();
        assertThat(dto.getOpphørsdatoRefusjon()).isEqualTo(java.time.LocalDate.of(9999, 12, 31));
        assertThat(dto.getArbeidsgiver().orgnr()).isEqualTo(orgnr);
    }

    @Test
    void skal_verifisere_at_mapTilDto_og_mapTilEntitet_gir_ekvivalente_resultater() {
        // Arrange
        var stp = LocalDate.now();
        var refusjonsendringer = List.of(
            new SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto(stp.plusDays(10), BigDecimal.valueOf(4000)),
            new SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto(stp.plusDays(15), BigDecimal.ZERO));
        var request = new SendOverstyrtInntektsmeldingRequestDto(new AktørIdDto("9999999999999"),
            YtelseTypeDto.FORELDREPENGER,
            new OrganisasjonsnummerDto("999999999"),
            stp,
            BigDecimal.valueOf(5000),
            BigDecimal.valueOf(5000),
            refusjonsendringer,
            Collections.emptyList(),
            "Truls Test",
            new SaksnummerDto("1234"));

        // Act
        var entitet = InntektsmeldingOverstyringMapper.mapTilEntitet(request);
        var dto = InntektsmeldingOverstyringMapper.mapTilDto(request);

        // Assert - verify equivalent data
        assertThat(dto.getAktørId().getAktørId()).isEqualTo(entitet.getAktørId().getAktørId());
        assertThat(dto.getArbeidsgiver().orgnr()).isEqualTo(entitet.getArbeidsgiverIdent());
        assertThat(dto.getMånedInntekt()).isEqualByComparingTo(entitet.getMånedInntekt());
        assertThat(dto.getStartdato()).isEqualTo(entitet.getStartDato());
        assertThat(dto.getYtelse()).isEqualTo(entitet.getYtelsetype());
        assertThat(dto.getMånedRefusjon()).isEqualByComparingTo(entitet.getMånedRefusjon());
        assertThat(dto.getOpphørsdatoRefusjon()).isEqualTo(entitet.getOpphørsdatoRefusjon());
        assertThat(dto.getOpprettetAv()).isEqualTo(entitet.getOpprettetAv());

        // Refusjonsendringer
        assertThat(dto.getSøkteRefusjonsperioder()).hasSize(entitet.getRefusjonsendringer().size());
        assertThat(dto.getSøkteRefusjonsperioder().getFirst().fom()).isEqualTo(entitet.getRefusjonsendringer().getFirst().getFom());
        assertThat(dto.getSøkteRefusjonsperioder().getFirst().beløp()).isEqualByComparingTo(entitet.getRefusjonsendringer().getFirst().getRefusjonPrMnd());
    }

    // ---- Tests for mapTilEntitet (SendOverstyrtInntektsmeldingRequestDto -> InntektsmeldingEntitet) - existing ----

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
