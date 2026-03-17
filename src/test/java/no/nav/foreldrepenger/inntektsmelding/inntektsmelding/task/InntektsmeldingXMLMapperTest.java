package no.nav.foreldrepenger.inntektsmelding.inntektsmelding.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.konfig.Tid;
import no.seres.xsd.nav.inntektsmelding_m._20181211.NaturalytelseDetaljer;

class InntektsmeldingXMLMapperTest {

    private static final LocalDate NOW = LocalDate.now();
    private static final String DUMMY_ARBEIDSGIVER_IDENT = "012345678";
    private static final AktørId DUMMY_AKTØRID = new AktørId("1234567890123");
    private static final String DUMMY_FNR = "12345678900";

    @Test
    void test_gjennoptatt_naturalytelse_ok() {
        var forventetNaturalytelseType = NaturalytelseType.BIL;
        var forventetBeløp = BigDecimal.valueOf(1000L);
        var forventetFom = NOW.plusDays(31);

        var inntektsmelding = lagInntektsmeldingDto(DUMMY_AKTØRID,
            List.of(lagBortfaltNaturalytelse(NOW, forventetFom, forventetNaturalytelseType, forventetBeløp)),
            Kildesystem.ARBEIDSGIVERPORTAL);

        var resultat = InntektsmeldingXMLMapper.map(inntektsmelding, PersonIdent.fra(DUMMY_FNR));

        var opphoerAvNaturalytelse = resultat.getSkjemainnhold().getOpphoerAvNaturalytelseListe().getValue().getOpphoerAvNaturalytelse();
        assertThat(opphoerAvNaturalytelse).hasSize(1);
        assertNaturalytelse(opphoerAvNaturalytelse.getFirst(), NOW, forventetNaturalytelseType, forventetBeløp);

        var gjennoptatteNaturalytelser = resultat.getSkjemainnhold().getGjenopptakelseNaturalytelseListe().getValue().getNaturalytelseDetaljer();
        assertThat(gjennoptatteNaturalytelser).hasSize(1);
        assertNaturalytelse(gjennoptatteNaturalytelser.getFirst(), forventetFom.plusDays(1), forventetNaturalytelseType, forventetBeløp);
    }

    @Test
    void test_bortfallt_naturalytelse_ok() {
        var forventetNaturalytelseType = NaturalytelseType.BIL;
        var forventetBeløp = BigDecimal.valueOf(1000L);
        var forventetFom = NOW;

        var inntektsmelding = lagInntektsmeldingDto(DUMMY_AKTØRID,
            List.of(lagBortfaltNaturalytelse(forventetFom, Tid.TIDENES_ENDE, forventetNaturalytelseType, forventetBeløp)),
            Kildesystem.ARBEIDSGIVERPORTAL);

        var resultat = InntektsmeldingXMLMapper.map(inntektsmelding, PersonIdent.fra(DUMMY_FNR));

        var opphoerAvNaturalytelse = resultat.getSkjemainnhold().getOpphoerAvNaturalytelseListe().getValue().getOpphoerAvNaturalytelse();
        assertThat(opphoerAvNaturalytelse).hasSize(1);
        assertNaturalytelse(opphoerAvNaturalytelse.getFirst(), forventetFom, forventetNaturalytelseType, forventetBeløp);

        assertThat(resultat.getSkjemainnhold().getGjenopptakelseNaturalytelseListe().getValue().getNaturalytelseDetaljer()).isEmpty();
    }

    @Test
    void test_overstyrt_inntektsmelding() {
        var inntektsmelding = lagInntektsmeldingDto(DUMMY_AKTØRID, List.of(), Kildesystem.FPSAK);

        var resultat = InntektsmeldingXMLMapper.map(inntektsmelding, PersonIdent.fra(DUMMY_FNR));

        assertThat(resultat.getSkjemainnhold().getAvsendersystem().getSystemnavn()).isEqualTo("OVERSTYRING_FPSAK");
    }

    private static void assertNaturalytelse(NaturalytelseDetaljer naturalytelseDetaljer,
                                            LocalDate forventetFom,
                                            NaturalytelseType forventetNaturalytelseType,
                                            BigDecimal forventetBeløp) {
        assertThat(naturalytelseDetaljer.getFom().getValue()).isEqualTo(forventetFom);
        assertThat(naturalytelseDetaljer.getNaturalytelseType().getValue()).isEqualTo(forventetNaturalytelseType.name().toLowerCase());
        assertThat(naturalytelseDetaljer.getBeloepPrMnd().getValue()).isEqualTo(forventetBeløp);
    }

    private static InntektsmeldingDto lagInntektsmeldingDto(AktørId aktørId,
                                                            List<InntektsmeldingDto.BortfaltNaturalytelse> naturalytelser,
                                                            Kildesystem kildesystem) {
        return InntektsmeldingDto.builder()
            .medAktørId(aktørId)
            .medArbeidsgiver(new Arbeidsgiver(DUMMY_ARBEIDSGIVER_IDENT))
            .medStartdato(NOW)
            .medInntekt(BigDecimal.ZERO)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medKildesystem(kildesystem)
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("11111111", "Test Person"))
            .medBortfaltNaturalytelsePerioder(naturalytelser)
            .medSøkteRefusjonsperioder(List.of())
            .build();
    }

    private static InntektsmeldingDto.BortfaltNaturalytelse lagBortfaltNaturalytelse(LocalDate fom,
                                                                                     LocalDate tom,
                                                                                     NaturalytelseType type,
                                                                                     BigDecimal beløp) {
        return new InntektsmeldingDto.BortfaltNaturalytelse(fom, tom, type, beløp);
    }
}
