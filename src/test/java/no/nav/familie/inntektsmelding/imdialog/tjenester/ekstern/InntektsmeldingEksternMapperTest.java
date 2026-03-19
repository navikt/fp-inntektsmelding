package no.nav.familie.inntektsmelding.imdialog.tjenester.ekstern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.imdialog.rest.ekstern.SendInntektsmeldingEksternRequest;
import no.nav.familie.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.familie.inntektsmelding.koder.Endringsårsak;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.EndringsårsakDto;
import no.nav.familie.inntektsmelding.typer.dto.NaturalytelsetypeDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.vedtak.konfig.Tid;

class InntektsmeldingEksternMapperTest {

    @Test
    void skal_mappe_til_entitet_med_refusjon_endring_naturalytelse_og_endringsarsak() {
        var startdato = LocalDate.of(2026, 1, 10);
        var request = lagEksternIMRequest(
            startdato,
            List.of(
                new SendInntektsmeldingEksternRequest.RefusjonRequest(startdato, BigDecimal.valueOf(10000)),
                new SendInntektsmeldingEksternRequest.RefusjonRequest(startdato.plusDays(5), BigDecimal.valueOf(9000)),
                new SendInntektsmeldingEksternRequest.RefusjonRequest(startdato.plusDays(10), BigDecimal.ZERO)
            )
        );

        var entitet = InntektsmeldingEksternMapper.mapTilEntitet(request, new AktørId("1234567891011"));

        assertThat(entitet.getAktørId().getAktørId()).isEqualTo("1234567891011");
        assertThat(entitet.getArbeidsgiverIdent()).isEqualTo("999999999");
        assertThat(entitet.getStartDato()).isEqualTo(startdato);
        assertThat(entitet.getYtelsetype()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(entitet.getKildesystem()).isEqualTo(Kildesystem.LØNN_OG_PERSONAL_SYSTEM);
        assertThat(entitet.getLpsSystemNavn()).isEqualTo("test-lps");
        assertThat(entitet.getLpsSystemVersjon()).isEqualTo("1.0.0");

        assertThat(entitet.getMånedInntekt()).isEqualByComparingTo(BigDecimal.valueOf(45000));
        assertThat(entitet.getMånedRefusjon()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(entitet.getOpphørsdatoRefusjon()).isEqualTo(startdato.plusDays(9));
        assertThat(entitet.getRefusjonsendringer()).hasSize(1);
        assertThat(entitet.getRefusjonsendringer().getFirst().getFom()).isEqualTo(startdato.plusDays(5));
        assertThat(entitet.getRefusjonsendringer().getFirst().getRefusjonPrMnd()).isEqualByComparingTo(BigDecimal.valueOf(9000));

        assertThat(entitet.getBorfalteNaturalYtelser()).hasSize(1);
        assertThat(entitet.getBorfalteNaturalYtelser().getFirst().getType()).isEqualTo(NaturalytelseType.BIL);
        assertThat(entitet.getBorfalteNaturalYtelser().getFirst().getMånedBeløp()).isEqualByComparingTo(BigDecimal.valueOf(1200));
        assertThat(entitet.getBorfalteNaturalYtelser().getFirst().getPeriode().getFom()).isEqualTo(startdato.plusDays(2));
        assertThat(entitet.getBorfalteNaturalYtelser().getFirst().getPeriode().getTom()).isEqualTo(Tid.TIDENES_ENDE);

        assertThat(entitet.getEndringsårsaker()).hasSize(1);
        assertThat(entitet.getEndringsårsaker().getFirst().getÅrsak()).isEqualTo(Endringsårsak.TARIFFENDRING);
        assertThat(entitet.getEndringsårsaker().getFirst().getBleKjentFom()).contains(startdato.plusDays(1));
        assertThat(entitet.getKontaktperson().getNavn()).isEqualTo("Kontakt Person");
        assertThat(entitet.getKontaktperson().getTelefonnummer()).isEqualTo("12345678");
    }

    @Test
    void skal_mappe_til_entitet_uten_refusjon() {
        var startdato = LocalDate.of(2026, 2, 1);
        var request = lagEksternIMRequest(startdato, List.of());

        var entitet = InntektsmeldingEksternMapper.mapTilEntitet(request, new AktørId("1234567891011"));

        assertThat(entitet.getMånedRefusjon()).isNull();
        assertThat(entitet.getOpphørsdatoRefusjon()).isNull();
        assertThat(entitet.getRefusjonsendringer()).isEmpty();
    }

    @Test
    void skal_feile_nar_det_finnes_mer_enn_en_refusjon_pa_startdato() {
        var startdato = LocalDate.of(2026, 2, 1);
        var request = lagEksternIMRequest(
            startdato,
            List.of(
                new SendInntektsmeldingEksternRequest.RefusjonRequest(startdato, BigDecimal.valueOf(10000)),
                new SendInntektsmeldingEksternRequest.RefusjonRequest(startdato, BigDecimal.valueOf(9000))
            )
        );

        assertThatThrownBy(() -> InntektsmeldingEksternMapper.mapTilEntitet(request, new AktørId("1234567891011")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Forventer kun 1 refusjon som starter på startdato");
    }

    private static SendInntektsmeldingEksternRequest lagEksternIMRequest(LocalDate startdato,
                                                                         List<SendInntektsmeldingEksternRequest.RefusjonRequest> refusjoner) {
        return new SendInntektsmeldingEksternRequest(
            UUID.randomUUID(),
            "12345678901",
            new OrganisasjonsnummerDto("999999999"),
            startdato,
            SendInntektsmeldingEksternRequest.YtelseTypeRequest.FORELDREPENGER,
            new SendInntektsmeldingEksternRequest.KontaktpersonRequest("Kontakt Person", "12345678"),
            BigDecimal.valueOf(45000),
            refusjoner,
            List.of(new SendInntektsmeldingEksternRequest.BortfaltNaturalytelseRequest(
                startdato.plusDays(2),
                null,
                NaturalytelsetypeDto.BIL,
                BigDecimal.valueOf(1200)
            )),
            List.of(new SendInntektsmeldingEksternRequest.EndringsårsakerRequest(
                EndringsårsakDto.TARIFFENDRING,
                null,
                null,
                startdato.plusDays(1)
            )),
            new SendInntektsmeldingEksternRequest.AvsenderSystemRequest("test-lps", "1.0.0", LocalDateTime.of(2026, 1, 2, 10, 30))
        );
    }
}

