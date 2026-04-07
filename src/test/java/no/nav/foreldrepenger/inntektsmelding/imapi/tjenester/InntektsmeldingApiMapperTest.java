package no.nav.foreldrepenger.inntektsmelding.imapi.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.ArbeidsgiverDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.FødselsnummerDto;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.AvsenderSystem;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.BortfaltNaturalytelse;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.Endringsårsaker;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.Kontaktperson;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.Naturalytelsetype;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.SendInntektsmeldingRequest;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.SøktRefusjon;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.YtelseType;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.tjenester.InntektsmeldingApiMapper;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Endringsårsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.konfig.Tid;

class InntektsmeldingApiMapperTest {

    @Test
    void skal_mappe_til_dto_med_refusjon_endring_naturalytelse_og_endringsarsak() {
        var startdato = LocalDate.of(2026, 1, 10);
        var request = lagEksternIMRequest(
            startdato,
            List.of(
                new SøktRefusjon(startdato, BigDecimal.valueOf(10000)),
                new SøktRefusjon(startdato.plusDays(5), BigDecimal.valueOf(9000)),
                new SøktRefusjon(startdato.plusDays(10), BigDecimal.ZERO)
            )
        );

        var dto = InntektsmeldingApiMapper.mapTilDto(request, new AktørId("1234567891011"));

        assertThat(dto.getAktørId().getAktørId()).isEqualTo("1234567891011");
        assertThat(dto.getArbeidsgiver().orgnr()).isEqualTo("999999999");
        assertThat(dto.getStartdato()).isEqualTo(startdato);
        assertThat(dto.getYtelse()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(dto.getKildesystem()).isEqualTo(Kildesystem.LØNN_OG_PERSONAL_SYSTEM);
        assertThat(dto.getAvsenderSystem().navn()).isEqualTo("test-lps");
        assertThat(dto.getAvsenderSystem().versjon()).isEqualTo("1.0.0");

        assertThat(dto.getMånedInntekt()).isEqualByComparingTo(BigDecimal.valueOf(45000));
        assertThat(dto.getMånedRefusjon()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(dto.getOpphørsdatoRefusjon()).isEqualTo(startdato.plusDays(9));
        assertThat(dto.getSøkteRefusjonsperioder()).hasSize(1);
        assertThat(dto.getSøkteRefusjonsperioder().getFirst().fom()).isEqualTo(startdato.plusDays(5));
        assertThat(dto.getSøkteRefusjonsperioder().getFirst().beløp()).isEqualByComparingTo(BigDecimal.valueOf(9000));

        assertThat(dto.getBortfaltNaturalytelsePerioder()).hasSize(1);
        assertThat(dto.getBortfaltNaturalytelsePerioder().getFirst().naturalytelsetype()).isEqualTo(NaturalytelseType.BIL);
        assertThat(dto.getBortfaltNaturalytelsePerioder().getFirst().beløp()).isEqualByComparingTo(BigDecimal.valueOf(1200));
        assertThat(dto.getBortfaltNaturalytelsePerioder().getFirst().fom()).isEqualTo(startdato.plusDays(2));
        assertThat(dto.getBortfaltNaturalytelsePerioder().getFirst().tom()).isEqualTo(Tid.TIDENES_ENDE);

        assertThat(dto.getEndringAvInntektÅrsaker()).hasSize(1);
        assertThat(dto.getEndringAvInntektÅrsaker().getFirst().årsak()).isEqualTo(Endringsårsak.TARIFFENDRING);
        assertThat(dto.getEndringAvInntektÅrsaker().getFirst().bleKjentFom()).isEqualTo(startdato.plusDays(1));
        assertThat(dto.getKontaktperson().navn()).isEqualTo("Kontakt Person");
        assertThat(dto.getKontaktperson().telefonnummer()).isEqualTo("12345678");
    }

    @Test
    void skal_mappe_til_dto_uten_refusjon() {
        var startdato = LocalDate.of(2026, 2, 1);
        var request = lagEksternIMRequest(startdato, List.of());

        var dto = InntektsmeldingApiMapper.mapTilDto(request, new AktørId("1234567891011"));

        assertThat(dto.getMånedRefusjon()).isNull();
        assertThat(dto.getOpphørsdatoRefusjon()).isNull();
        assertThat(dto.getSøkteRefusjonsperioder()).isEmpty();
    }

    @Test
    void skal_feile_nar_det_finnes_mer_enn_en_refusjon_pa_startdato() {
        var startdato = LocalDate.of(2026, 2, 1);
        var request = lagEksternIMRequest(
            startdato,
            List.of(
                new SøktRefusjon(startdato, BigDecimal.valueOf(10000)),
                new SøktRefusjon(startdato, BigDecimal.valueOf(9000))
            )
        );

        assertThatThrownBy(() -> InntektsmeldingApiMapper.mapTilDto(request, new AktørId("1234567891011")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Forventer kun 1 refusjon som starter på startdato");
    }

    private static SendInntektsmeldingRequest lagEksternIMRequest(LocalDate startdato,
                                                                  List<SøktRefusjon> refusjoner) {
        return new SendInntektsmeldingRequest(
            UUID.randomUUID(),
            new FødselsnummerDto("12345678910"),
            new ArbeidsgiverDto("999999999"),
            startdato,
            YtelseType.FORELDREPENGER,
            new Kontaktperson("Kontakt Person", "12345678"),
            BigDecimal.valueOf(45000),
            refusjoner,
            List.of(new BortfaltNaturalytelse(
                startdato.plusDays(2),
                null,
                Naturalytelsetype.BIL,
                BigDecimal.valueOf(1200)
            )),
            List.of(new Endringsårsaker(
                no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.Endringsårsak.TARIFFENDRING,
                null,
                null,
                startdato.plusDays(1)
            )),
            new AvsenderSystem("test-lps", "1.0.0")
        );
    }
}
