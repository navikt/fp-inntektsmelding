package no.nav.familie.inntektsmelding.imdialog.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import no.nav.familie.inntektsmelding.koder.Kildesystem;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;
import no.seres.xsd.nav.inntektsmelding_m._20181211.NaturalytelseDetaljer;

class InntektsmeldingXMLMapperTest {

    private static final LocalDate NOW = LocalDate.now();
    private static final String DUMMY_ARBEIDSGIVER_IDENT = "0123456789";
    private static final AktørIdEntitet DUMMY_AKTØRID = AktørIdEntitet.dummy();
    private static final String DUMMY_FNR = "12345678900";

    @Test
    void test_gjennoptatt_naturalytelse_ok() {
        var forventetNaturalytelseType = NaturalytelseType.BIL;
        var forventetBeløp = BigDecimal.valueOf(1000L);
        var forventetFom = NOW.plusDays(31);

        var aktøridFnrMap = Map.of(DUMMY_AKTØRID, PersonIdent.fra(DUMMY_FNR));

        var inntektsmelding = lagInntektsmeldingEntitet(DUMMY_AKTØRID,
            List.of(lagBortfaltNaturalytelseEntitet(NOW, forventetFom, forventetNaturalytelseType, forventetBeløp)), Kildesystem.ARBEIDSGIVERPORTAL);

        var resultat = InntektsmeldingXMLMapper.map(inntektsmelding, aktøridFnrMap);

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

        var aktøridFnrMap = Map.of(DUMMY_AKTØRID, PersonIdent.fra(DUMMY_FNR));

        var inntektsmelding = lagInntektsmeldingEntitet(DUMMY_AKTØRID,
            List.of(lagBortfaltNaturalytelseEntitet(forventetFom, Tid.TIDENES_ENDE, forventetNaturalytelseType, forventetBeløp)), Kildesystem.ARBEIDSGIVERPORTAL
        );

        var resultat = InntektsmeldingXMLMapper.map(inntektsmelding, aktøridFnrMap);

        var opphoerAvNaturalytelse = resultat.getSkjemainnhold().getOpphoerAvNaturalytelseListe().getValue().getOpphoerAvNaturalytelse();
        assertThat(opphoerAvNaturalytelse).hasSize(1);
        assertNaturalytelse(opphoerAvNaturalytelse.getFirst(), forventetFom, forventetNaturalytelseType, forventetBeløp);

        assertThat(resultat.getSkjemainnhold().getGjenopptakelseNaturalytelseListe().getValue().getNaturalytelseDetaljer()).isEmpty();
    }

    @Test
    void test_overstyrt_inntektsmelding() {
        var aktøridFnrMap = Map.of(DUMMY_AKTØRID, PersonIdent.fra(DUMMY_FNR));

        var inntektsmelding = lagInntektsmeldingEntitet(DUMMY_AKTØRID, List.of(), Kildesystem.FPSAK);

        var resultat = InntektsmeldingXMLMapper.map(inntektsmelding, aktøridFnrMap);

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

    private static InntektsmeldingEntitet lagInntektsmeldingEntitet(AktørIdEntitet aktørId,
                                                                    List<BortaltNaturalytelseEntitet> bortfaltNaturalytelseEntitet, Kildesystem kildesystem) {
        return InntektsmeldingEntitet.builder()
            .medBortfaltNaturalytelser(bortfaltNaturalytelseEntitet)
            .medArbeidsgiverIdent(DUMMY_ARBEIDSGIVER_IDENT)
            .medAktørId(aktørId)
            .medStartDato(NOW)
            .medMånedInntekt(BigDecimal.ZERO)
            .medKildesystem(kildesystem)
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .build();
    }

    private static BortaltNaturalytelseEntitet lagBortfaltNaturalytelseEntitet(LocalDate fom,
                                                                               LocalDate tom,
                                                                               NaturalytelseType type,
                                                                               BigDecimal beløp) {
        return BortaltNaturalytelseEntitet.builder()
            .medPeriode(fom, tom)
            .medType(type)
            .medMånedBeløp(beløp)
            .build();
    }
}
