package no.nav.foreldrepenger.inntektsmelding.utils.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.vedtak.konfig.Tid;

class NaturalYtelseTypeMapperTest {

    protected static final BigDecimal BELØP = BigDecimal.valueOf(1000);

    @Test
    void map_riktig_bortfallt_ytelse_fult() {

        var naturalYtelseEntiteter = List.of(
            lagBortfaltYtelse(0, null, NaturalytelseType.BIL, BELØP),
            lagBortfaltYtelse(10, null, NaturalytelseType.BOLIG, BELØP)
        );

        var resultat = NaturalYtelseMapper.mapNaturalYtelser(naturalYtelseEntiteter);

        assertThat(resultat).isNotNull().isNotEmpty().hasSize(2).allSatisfy(ytelse -> {
            assertTrue(ytelse.bortfallt());
            assertThat(ytelse.beløp()).isEqualTo(BELØP);
        });
    }

    @Test
    void map_riktig_bortfallt_ytelse_delvis() {

        var naturalYtelseEntiteter = List.of(
            lagBortfaltYtelse(0, 10, NaturalytelseType.BIL, BELØP),
            lagBortfaltYtelse(10, 20, NaturalytelseType.BOLIG, BELØP)
        );

        var resultat = NaturalYtelseMapper.mapNaturalYtelser(naturalYtelseEntiteter);

        assertThat(resultat).isNotNull().isNotEmpty().hasSize(4);

        // Bortfalt
        assertThat(resultat.stream().filter(NaturalYtelseMapper.NaturalYtelse::bortfallt)).hasSize(2).allSatisfy(ytelse -> {
            assertTrue(ytelse.bortfallt());
            assertThat(ytelse.beløp()).isEqualTo(BELØP);
        });

        // Tilkommet
        assertThat(resultat.stream().filter(ytelse -> !ytelse.bortfallt())).hasSize(2).allSatisfy(ytelse -> {
            assertFalse(ytelse.bortfallt());
            assertThat(ytelse.beløp()).isEqualTo(BELØP);
        });
    }

    @Test
    void map_riktig_bortfallt_ytelse_overlapp() {

        var naturalYtelseEntiteter = List.of(
            lagBortfaltYtelse(0, 10, NaturalytelseType.BIL, BELØP),
            lagBortfaltYtelse(5, 20, NaturalytelseType.BIL, BELØP)
        );

        var resultat = NaturalYtelseMapper.mapNaturalYtelser(naturalYtelseEntiteter);

        assertThat(resultat).isNotNull().isNotEmpty().hasSize(4);

        // Bortfalt
        assertThat(resultat.stream().filter(NaturalYtelseMapper.NaturalYtelse::bortfallt)).hasSize(2).allSatisfy(ytelse -> {
            assertTrue(ytelse.bortfallt());
            assertThat(ytelse.beløp()).isEqualTo(BELØP);
        });

        // Tilkommet
        assertThat(resultat.stream().filter(ytelse -> !ytelse.bortfallt())).hasSize(2).allSatisfy(ytelse -> {
            assertFalse(ytelse.bortfallt());
            assertThat(ytelse.beløp()).isEqualTo(BELØP);
        });
    }

    private InntektsmeldingDto.BortfaltNaturalytelse lagBortfaltYtelse(int fom, Integer tom, NaturalytelseType naturalytelseType, BigDecimal beløp) {
        var now = LocalDate.now();
        return new InntektsmeldingDto.BortfaltNaturalytelse(
            now.plusDays(fom),
            tom == null ? Tid.TIDENES_ENDE : now.plusDays(tom),
            naturalytelseType,
            beløp);
    }
}
