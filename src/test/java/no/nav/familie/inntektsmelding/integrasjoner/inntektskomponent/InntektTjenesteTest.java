package no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;

import no.nav.vedtak.exception.IntegrasjonException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.tjenester.aordningen.inntektsinformasjon.Aktoer;
import no.nav.tjenester.aordningen.inntektsinformasjon.AktoerType;
import no.nav.tjenester.aordningen.inntektsinformasjon.ArbeidsInntektIdent;
import no.nav.tjenester.aordningen.inntektsinformasjon.ArbeidsInntektInformasjon;
import no.nav.tjenester.aordningen.inntektsinformasjon.ArbeidsInntektMaaned;
import no.nav.tjenester.aordningen.inntektsinformasjon.inntekt.Inntekt;
import no.nav.tjenester.aordningen.inntektsinformasjon.inntekt.InntektType;
import no.nav.tjenester.aordningen.inntektsinformasjon.response.HentInntektListeBolkResponse;

@ExtendWith(MockitoExtension.class)
class InntektTjenesteTest {
    private static final String ORGNR = "111111111";
    private static final String AKTØR_ID = "9999999999999";

    @Mock
    private InntektskomponentKlient klient;
    private InntektTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new InntektTjeneste(klient);
    }

    @Test
    void skal_teste_at_inntekter_innhentes_happy_case() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = stp.plusDays(10);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 7), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt1 = getInntekt(YearMonth.of(2024,7), BigDecimal.valueOf(25_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,8), BigDecimal.valueOf(25_000));
        var inntekt3 = getInntekt(YearMonth.of(2024,9), BigDecimal.valueOf(25_000));
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt1, inntekt2, inntekt3));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntektsopplysinger = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 7), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 8), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 9), MånedslønnStatus.BRUKT_I_GJENNOMSNITT));
        assertResultat(inntektsopplysinger, forventetListe, ORGNR, BigDecimal.valueOf(25_000));
    }

    @Test
    void skal_teste_at_inntekter_ikke_finnes() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = LocalDate.of(2024,10,15);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 7), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        aiResponse.setArbeidsInntektMaaned(null);
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntektsopplysinger = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 7), MånedslønnStatus.IKKE_RAPPORTERT_MEN_BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 8), MånedslønnStatus.IKKE_RAPPORTERT_MEN_BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 9), MånedslønnStatus.IKKE_RAPPORTERT_MEN_BRUKT_I_GJENNOMSNITT));
        assertResultat(inntektsopplysinger, forventetListe, ORGNR, BigDecimal.ZERO);
    }

    @Test
    void skal_teste_at_inntekter_innhentes_dagens_dato_er_før_rapporteringsfrist_og_inntekt_ikke_finnes_for_siste_måned() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = LocalDate.of(2024,10,1);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 6), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt1 = getInntekt(YearMonth.of(2024,6), BigDecimal.valueOf(25_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,7), BigDecimal.valueOf(25_000));
        var inntekt3 = getInntekt(YearMonth.of(2024,8), BigDecimal.valueOf(25_000));
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt1, inntekt2, inntekt3));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntektsopplysinger = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 6), MånedslønnStatus.BRUKT_I_GJENNOMSNITT),
            new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 7), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 8), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 9), MånedslønnStatus.IKKE_RAPPORTERT_RAPPORTERINGSFRIST_IKKE_PASSERT));
        assertResultat(inntektsopplysinger, forventetListe, ORGNR, BigDecimal.valueOf(25_000));
    }

    @Test
    void skal_teste_at_inntekter_innhentes_dagens_dato_er_før_rapporteringsfrist_og_inntekt_finnes_for_siste_måned() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = LocalDate.of(2024,10,1);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 6), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt1 = getInntekt(YearMonth.of(2024,6), BigDecimal.valueOf(25_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,7), BigDecimal.valueOf(25_000));
        var inntekt3 = getInntekt(YearMonth.of(2024,8), BigDecimal.valueOf(25_000));
        var inntekt4 = getInntekt(YearMonth.of(2024,9), BigDecimal.valueOf(25_000));
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt1, inntekt2, inntekt3, inntekt4));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);


        var inntektsopplysinger = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 7), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 8), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 9), MånedslønnStatus.BRUKT_I_GJENNOMSNITT));
        assertResultat(inntektsopplysinger, forventetListe, ORGNR, BigDecimal.valueOf(25_000));
    }

    @Test
    void skal_teste_at_inntekter_innhentes_når_det_mangler_inntekt_midt_i_perioden_uten_justering() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = LocalDate.of(2024,10,15);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 7), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt1 = getInntekt(YearMonth.of(2024,7), BigDecimal.valueOf(25_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,8));
        var inntekt3 = getInntekt(YearMonth.of(2024,9), BigDecimal.valueOf(25_000));
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt1, inntekt2, inntekt3));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntektsopplysinger = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 7), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 8), MånedslønnStatus.IKKE_RAPPORTERT_MEN_BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 9), MånedslønnStatus.BRUKT_I_GJENNOMSNITT));
        assertResultat(inntektsopplysinger, forventetListe, ORGNR, BigDecimal.valueOf(16_666.67));
    }

    @Test
    void skal_teste_at_inntekter_innhentes_når_det_mangler_inntekt_midt_i_perioden_med_justering() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = LocalDate.of(2024,10,2);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 6), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt1 = getInntekt(YearMonth.of(2024,6), BigDecimal.valueOf(25_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,7));
        var inntekt3 = getInntekt(YearMonth.of(2024,8), BigDecimal.valueOf(25_000));
        var inntekt4 = getInntekt(YearMonth.of(2024,9));
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt1, inntekt2, inntekt3, inntekt4));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntektsopplysinger = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 6), MånedslønnStatus.BRUKT_I_GJENNOMSNITT),
            new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 7), MånedslønnStatus.IKKE_RAPPORTERT_MEN_BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 8), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 9), MånedslønnStatus.IKKE_RAPPORTERT_RAPPORTERINGSFRIST_IKKE_PASSERT));
        assertResultat(inntektsopplysinger, forventetListe, ORGNR, BigDecimal.valueOf(16_666.67));
    }

    @Test
    void skal_teste_når_ingen_inntekter_er_rapportert() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = LocalDate.of(2024,10,15);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 7), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        aiResponse.setArbeidsInntektMaaned(List.of());
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntektsopplysinger = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 7), MånedslønnStatus.IKKE_RAPPORTERT_MEN_BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 8), MånedslønnStatus.IKKE_RAPPORTERT_MEN_BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 9), MånedslønnStatus.IKKE_RAPPORTERT_MEN_BRUKT_I_GJENNOMSNITT));
        assertResultat(inntektsopplysinger, forventetListe, ORGNR, BigDecimal.ZERO);
    }

    @Test
    void skal_teste_at_inntekter_innhentes_dagens_dato_er_før_rapporteringsfrist_og_4_uker_før_stp() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,12,1);
        var dagensDato = LocalDate.of(2024,11,4);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 7), YearMonth.of(2024, 11));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt1 = getInntekt(YearMonth.of(2024,7), BigDecimal.valueOf(20_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,8), BigDecimal.valueOf(25_000));
        var inntekt3 = getInntekt(YearMonth.of(2024,9), BigDecimal.valueOf(30_000));
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt1, inntekt2, inntekt3));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntektsopplysinger = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(20_000), YearMonth.of(2024, 7), MånedslønnStatus.BRUKT_I_GJENNOMSNITT),
            new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 8), MånedslønnStatus.BRUKT_I_GJENNOMSNITT),
            new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(30_000), YearMonth.of(2024, 9), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 10), MånedslønnStatus.IKKE_RAPPORTERT_RAPPORTERINGSFRIST_IKKE_PASSERT)
            , new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 11), MånedslønnStatus.IKKE_RAPPORTERT_RAPPORTERINGSFRIST_IKKE_PASSERT));
        assertResultat(inntektsopplysinger, forventetListe, ORGNR, BigDecimal.valueOf(25_000));
    }

    @Test
    void skal_teste_at_inntekter_innhentes_dagens_dato_er_før_rapporteringsfrist_og_4_uker_før_stp_men_midterste_måned_finnes() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,12,1);
        var dagensDato = LocalDate.of(2024,11,4);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 7), YearMonth.of(2024, 11));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt1 = getInntekt(YearMonth.of(2024,7), BigDecimal.valueOf(20_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,8), BigDecimal.valueOf(25_000));
        var inntekt3 = getInntekt(YearMonth.of(2024,9), BigDecimal.valueOf(30_000));
        var inntekt4 = getInntekt(YearMonth.of(2024,10), BigDecimal.valueOf(30_000));
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt1, inntekt2, inntekt3, inntekt4));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntektsopplysinger = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 8), MånedslønnStatus.BRUKT_I_GJENNOMSNITT),
            new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(30_000), YearMonth.of(2024, 9), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(30_000), YearMonth.of(2024, 10), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 11), MånedslønnStatus.IKKE_RAPPORTERT_RAPPORTERINGSFRIST_IKKE_PASSERT));
        assertResultat(inntektsopplysinger, forventetListe, ORGNR, BigDecimal.valueOf(28_333.33));
    }

    @Test
    void skal_teste_flere_inntekter_pr_måned() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = stp.plusDays(10);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 7), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt1 = getInntekt(YearMonth.of(2024,7), BigDecimal.valueOf(25_000), BigDecimal.valueOf(10_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,8), BigDecimal.valueOf(25_000), BigDecimal.valueOf(5_000));
        var inntekt3 = getInntekt(YearMonth.of(2024,9), BigDecimal.valueOf(25_000));
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt1, inntekt2, inntekt3));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntektsopplysinger = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(35_000), YearMonth.of(2024, 7), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(30_000), YearMonth.of(2024, 8), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 9), MånedslønnStatus.BRUKT_I_GJENNOMSNITT));
        assertResultat(inntektsopplysinger, forventetListe, ORGNR, BigDecimal.valueOf(30_000));
    }

    @Test
    void skal_teste_flere_inntekter_pr_måned_noen_null() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = stp.plusDays(10);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 7), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt1 = getInntekt(YearMonth.of(2024,7), BigDecimal.valueOf(25_000), BigDecimal.valueOf(10_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,8), BigDecimal.valueOf(25_000), BigDecimal.valueOf(5_000), null);
        var inntekt3 = getInntekt(YearMonth.of(2024,9), BigDecimal.valueOf(25_000), null);
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt1, inntekt2, inntekt3));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntektsopplysinger = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(35_000), YearMonth.of(2024, 7), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(30_000), YearMonth.of(2024, 8), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 9), MånedslønnStatus.BRUKT_I_GJENNOMSNITT));
        assertResultat(inntektsopplysinger, forventetListe, ORGNR, BigDecimal.valueOf(30_000));
    }

    @Test
    void skal_fjerne_alle_eldre_måneder_uten_inntekt_når_tre_nyeste_er_rapportert() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,12,15);
        var dagensDato = LocalDate.of(2024,11,18);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 8), YearMonth.of(2024, 11));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt0 = getInntekt(YearMonth.of(2024,8));
        var inntekt1 = getInntekt(YearMonth.of(2024,9), BigDecimal.valueOf(25_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,10), BigDecimal.valueOf(25_000));
        var inntekt3 = getInntekt(YearMonth.of(2024,11), BigDecimal.valueOf(25_000));
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt0, inntekt1, inntekt2, inntekt3));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntektsopplysinger = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 9), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 10), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(25_000), YearMonth.of(2024, 11), MånedslønnStatus.BRUKT_I_GJENNOMSNITT));
        assertResultat(inntektsopplysinger, forventetListe, ORGNR, BigDecimal.valueOf(25_000));
    }

    @Test
    void skal_returnere_tomme_inntekter_hvis_inntektskomponenten_er_nede() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,12,15);
        var dagensDato = LocalDate.of(2024,11,18);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 8), YearMonth.of(2024, 11));

        when(klient.finnInntekt(forventetRequest)).thenThrow(new IntegrasjonException("TESTMELDING", "Noe feil"));

        var inntektsopplysinger = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 9), MånedslønnStatus.NEDETID_AINNTEKT)
            , new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 10), MånedslønnStatus.NEDETID_AINNTEKT)
            , new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 11), MånedslønnStatus.NEDETID_AINNTEKT));
        assertResultat(inntektsopplysinger, forventetListe, ORGNR, null);
    }

    @Test
    void skal_teste_negative_inntekter_blir_til_0() {
        var aktørId = new AktørIdEntitet(AKTØR_ID);
        var stp = LocalDate.of(2024,10,15);
        var dagensDato = stp.plusDays(10);
        var forventetRequest = new FinnInntektRequest(aktørId.getAktørId(), YearMonth.of(2024, 7), YearMonth.of(2024, 9));

        var response = new HentInntektListeBolkResponse();
        var aiResponse = new ArbeidsInntektIdent();
        aiResponse.setIdent(new Aktoer(aktørId.getAktørId(), AktoerType.AKTOER_ID));
        var inntekt1 = getInntekt(YearMonth.of(2024,7), BigDecimal.valueOf(3_000));
        var inntekt2 = getInntekt(YearMonth.of(2024,8), BigDecimal.valueOf(-6_000));
        var inntekt3 = getInntekt(YearMonth.of(2024,9));
        aiResponse.setArbeidsInntektMaaned(List.of(inntekt1, inntekt2, inntekt3));
        response.setArbeidsInntektIdentListe(Collections.singletonList(aiResponse));
        when(klient.finnInntekt(forventetRequest)).thenReturn(response);

        var inntektsopplysinger = tjeneste.hentInntekt(aktørId, stp, dagensDato, ORGNR);

        var forventetListe = List.of(new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(3000), YearMonth.of(2024, 7), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(-6000), YearMonth.of(2024, 8), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)
            , new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2024, 9), MånedslønnStatus.IKKE_RAPPORTERT_MEN_BRUKT_I_GJENNOMSNITT));
        assertResultat(inntektsopplysinger, forventetListe, ORGNR, BigDecimal.valueOf(0));
    }

    private void assertResultat(Inntektsopplysninger inntektsopplysinger,
                                List<Inntektsopplysninger.InntektMåned> forventetListe,
                                String orgnr,
                                BigDecimal forventetSnittlønn) {
        assertThat(inntektsopplysinger).isNotNull();
        assertThat(inntektsopplysinger.orgnummer()).isEqualTo(orgnr);
        if (forventetSnittlønn == null) {
            assertThat(inntektsopplysinger.gjennomsnitt()).isNull();
        } else {
            assertThat(inntektsopplysinger.gjennomsnitt()).isEqualByComparingTo(forventetSnittlønn);
        }
        assertThat(inntektsopplysinger.måneder()).hasSameSizeAs(forventetListe);
        assertThat(inntektsopplysinger.måneder()).containsAll(forventetListe);
    }

    private static ArbeidsInntektMaaned getInntekt(YearMonth årMåned, BigDecimal... beløp) {
        var inntektMånedResponse = new ArbeidsInntektMaaned();
        inntektMånedResponse.setAarMaaned(årMåned);
        List<BigDecimal> alleBeløp = beløp == null ? Collections.emptyList() : Arrays.asList(beløp);
        var alleInntekter = alleBeløp.stream().map(b -> {
                var inntekt = new Inntekt();
                inntekt.setVirksomhet(new Aktoer(ORGNR, AktoerType.ORGANISASJON));
                inntekt.setInntektType(InntektType.LOENNSINNTEKT);
                inntekt.setUtbetaltIMaaned(årMåned);
                inntekt.setBeloep(b);
                return inntekt;
            })
            .toList();
        var aiInformasjon = new ArbeidsInntektInformasjon();
        aiInformasjon.setInntektListe(alleInntekter);
        inntektMånedResponse.setArbeidsInntektInformasjon(aiInformasjon);
        return inntektMånedResponse;
    }

}
