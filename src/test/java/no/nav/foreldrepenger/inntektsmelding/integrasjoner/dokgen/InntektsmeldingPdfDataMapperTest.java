package no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen;

import static no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterDatoForLister;
import static no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterDatoMedNavnPåUkedag;
import static no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterDatoOgTidNorsk;
import static no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterPersonnummer;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingPdfDataMapperTest {
    private static final String FORNAVN = "Test";
    private static final String MELLOMNAVN = "Tester";
    private static final String ETTERNAVN = "Testesen";
    private static final String ARBEIDSGIVER_IDENT = "999999999";
    private static final String ARBEIDSGIVER_NAVN = "Arbeidsgvier 1";
    private static final String NAVN = "Kontaktperson navn";
    private static final String ORG_NUMMER = "999999999";
    private static final BigDecimal REFUSJON_BELØP = BigDecimal.valueOf(35000);
    private static final no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId AKTØRID_SØKER = new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId("1234567891234");
    private static final BigDecimal INNTEKT = BigDecimal.valueOf(40000);
    private static final LocalDateTime OPPRETTETT_TIDSPUNKT = LocalDateTime.now();
    private static final LocalDate START_DATO = LocalDate.of(2024, 5, 1);
    private PersonInfo personInfo;
    private PersonIdent personIdent;

    @BeforeEach
    void setUP() {
        personIdent = new PersonIdent("11111111111");
        personInfo = new PersonInfo(FORNAVN, MELLOMNAVN, ETTERNAVN, personIdent, AKTØRID_SØKER, LocalDate.now(), null, null);
    }

    @Test
    void skal_opprette_pdfData() {
        var naturalytelseFraDato = LocalDate.of(2024, 6, 10);
        var naturalytelseBeløp = BigDecimal.valueOf(2000);
        var naturalytelse = new InntektsmeldingDto.BortfaltNaturalytelse(
            naturalytelseFraDato, Tid.TIDENES_ENDE,
            NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS, naturalytelseBeløp);

        var inntektsmeldingDto = lagStandardInntektsmeldingDtoBuilder()
            .medBortfaltNaturalytelsePerioder(List.of(naturalytelse))
            .build();

        var pdfData = InntektsmeldingPdfDataMapper.mapInntektsmeldingPdfData(inntektsmeldingDto, ARBEIDSGIVER_NAVN, personInfo, ARBEIDSGIVER_IDENT, ForespørselType.BESTILT_AV_FAGSYSTEM);

        assertThat(pdfData.getArbeidsgiverIdent()).isEqualTo(ARBEIDSGIVER_IDENT);
        assertThat(pdfData.getAvsenderSystem()).isEqualTo("NAV_NO");
        assertThat(pdfData.getArbeidsgiverNavn()).isEqualTo(ARBEIDSGIVER_NAVN);
        assertThat(pdfData.getKontaktperson().navn()).isEqualTo(NAVN);
        assertThat(pdfData.getKontaktperson().telefonnummer()).isEqualTo(ORG_NUMMER);
        assertThat(pdfData.getMånedInntekt()).isEqualTo(INNTEKT);
        assertThat(pdfData.getNavnSøker()).isEqualTo(FORNAVN + " " + MELLOMNAVN + " " + ETTERNAVN);
        assertThat(pdfData.getYtelsetype()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(pdfData.getOpprettetTidspunkt()).isEqualTo(formaterDatoOgTidNorsk(OPPRETTETT_TIDSPUNKT));
        assertThat(pdfData.getStartDato()).isEqualTo(formaterDatoMedNavnPåUkedag(START_DATO));
        assertThat(pdfData.getPersonnummer()).isEqualTo(formaterPersonnummer(personIdent.getIdent()));
        assertThat(pdfData.getRefusjonsendringer()).hasSize(1);
        assertThat(pdfData.getAntallRefusjonsperioder()).isEqualTo(1);
        assertThat(pdfData.getRefusjonsendringer().getFirst().beloep()).isEqualTo(REFUSJON_BELØP);
        assertThat(pdfData.getRefusjonsendringer().getFirst().fom()).isEqualTo(formaterDatoForLister(START_DATO));
        assertThat(pdfData.ingenGjenopptattNaturalytelse()).isTrue();
        assertThat(pdfData.ingenBortfaltNaturalytelse()).isFalse();
        assertThat(pdfData.getNaturalytelser().getFirst().fom()).isEqualTo(formaterDatoForLister(naturalytelseFraDato));
        assertThat(pdfData.getNaturalytelser().getFirst().beloep()).isEqualTo(naturalytelseBeløp);
        assertThat(pdfData.getNaturalytelser().getFirst().naturalytelseType()).isEqualTo("Aksjer grunnfondsbevis til underkurs");
    }

    @Test
    void skal_mappe_flere_refusjonsendringer_korrekt() {
        var refusjonsstartdato2 = LocalDate.now().plusWeeks(1);
        var refusjonsstartdato3 = refusjonsstartdato2.plusWeeks(2).plusDays(1);

        var refusjonsbeløp2 = BigDecimal.valueOf(34000);
        var refusjonsbeløp3 = BigDecimal.valueOf(32000);
        var refusjonsendringer = List.of(
            new InntektsmeldingDto.SøktRefusjon(refusjonsstartdato2, refusjonsbeløp2),
            new InntektsmeldingDto.SøktRefusjon(refusjonsstartdato3, refusjonsbeløp3));

        var inntektsmeldingDto = lagStandardInntektsmeldingDtoBuilder()
            .medMånedRefusjon(REFUSJON_BELØP)
            .medSøkteRefusjonsperioder(refusjonsendringer)
            .build();

        var pdfData = InntektsmeldingPdfDataMapper.mapInntektsmeldingPdfData(inntektsmeldingDto, ARBEIDSGIVER_NAVN, personInfo, ARBEIDSGIVER_IDENT, ForespørselType.BESTILT_AV_FAGSYSTEM);

        assertThat(pdfData.getRefusjonsendringer()).hasSize(3);
        assertThat(pdfData.getRefusjonsendringer().getFirst().beloep()).isEqualTo(REFUSJON_BELØP);
        assertThat(pdfData.getRefusjonsendringer().getFirst().fom()).isEqualTo(formaterDatoForLister(START_DATO));
        assertThat(pdfData.getRefusjonsendringer().get(1).beloep()).isEqualTo(refusjonsbeløp2);
        assertThat(pdfData.getRefusjonsendringer().get(1).fom()).isEqualTo(formaterDatoForLister(refusjonsstartdato2));
        assertThat(pdfData.getRefusjonsendringer().get(2).beloep()).isEqualTo(refusjonsbeløp3);
        assertThat(pdfData.getRefusjonsendringer().get(2).fom()).isEqualTo(formaterDatoForLister(refusjonsstartdato3));
        assertThat(pdfData.getAntallRefusjonsperioder()).isEqualTo(3);

        assertThat(pdfData.ingenGjenopptattNaturalytelse()).isTrue();
        assertThat(pdfData.ingenBortfaltNaturalytelse()).isTrue();
        assertThat(pdfData.getNaturalytelser()).isEmpty();
    }

    @Test
    void skal_mappe_flere_tilkommet_naturalytelser_av_samme_type_korrekt() {
        var naturalytelseFraDato = LocalDate.of(2024, 6, 1);
        var naturalytelseTilDato = LocalDate.of(2024, 7, 1);
        var naturalytelseAndreFraDato = naturalytelseTilDato.plusWeeks(1);
        var naturalytelseAndreTilDato = naturalytelseTilDato.plusWeeks(4);
        var naturalytelseTredjeTilDato = naturalytelseTilDato.plusWeeks(6);
        var naturalytelseBeløp = BigDecimal.valueOf(1000);

        var naturalytelser = List.of(
            new InntektsmeldingDto.BortfaltNaturalytelse(
                naturalytelseFraDato, naturalytelseTilDato,
                NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS, naturalytelseBeløp),
            new InntektsmeldingDto.BortfaltNaturalytelse(
                naturalytelseAndreFraDato, naturalytelseAndreTilDato,
                NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS, naturalytelseBeløp),
            new InntektsmeldingDto.BortfaltNaturalytelse(
                naturalytelseTredjeTilDato, Tid.TIDENES_ENDE,
                NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS, naturalytelseBeløp));

        var inntektsmeldingDto = lagStandardInntektsmeldingDtoBuilder()
            .medBortfaltNaturalytelsePerioder(naturalytelser)
            .build();

        var pdfData = InntektsmeldingPdfDataMapper.mapInntektsmeldingPdfData(inntektsmeldingDto, ARBEIDSGIVER_NAVN, personInfo, ARBEIDSGIVER_IDENT, ForespørselType.BESTILT_AV_FAGSYSTEM);

        assertThat(pdfData.ingenGjenopptattNaturalytelse()).isFalse();
        assertThat(pdfData.ingenBortfaltNaturalytelse()).isFalse();
        assertThat(pdfData.getNaturalytelser()).hasSize(5);

        var bortfalteNaturalytelser = pdfData.getNaturalytelser().stream().filter(NaturalYtelse::erBortfalt).toList();
        assertThat(bortfalteNaturalytelser).hasSize(3);

        var forventetFørsteFraDato = naturalytelseTilDato.plusDays(1);
        var forventetAndreFraDato = naturalytelseAndreTilDato.plusDays(1);

        var tilkomneNaturalytelser = pdfData.getNaturalytelser().stream().filter(naturalytelse -> !naturalytelse.erBortfalt()).toList();

        assertThat(tilkomneNaturalytelser).hasSize(2);
        assertThat(tilkomneNaturalytelser.getFirst().fom()).isEqualTo(formaterDatoForLister(forventetFørsteFraDato));
        assertThat(tilkomneNaturalytelser.get(1).fom()).isEqualTo(formaterDatoForLister(forventetAndreFraDato));
    }

    @Test
    void skal_mappe_naturalytelser_av_ulik_type_korrekt() {
        var naturalytelseFraDato = LocalDate.of(2024, 6, 1);
        var naturalytelseTilDato = LocalDate.of(2024, 7, 1);
        var naturalytelseAndreFraDato = naturalytelseTilDato.plusWeeks(1);
        var naturalytelseAndreTilDato = naturalytelseTilDato.plusWeeks(4);
        var naturalytelseTredjeTilDato = naturalytelseTilDato.plusWeeks(6);
        var naturalytelseBeløp = BigDecimal.valueOf(1000);

        var naturalytelser = List.of(
            new InntektsmeldingDto.BortfaltNaturalytelse(
                naturalytelseFraDato, naturalytelseTilDato,
                NaturalytelseType.BIL, naturalytelseBeløp),
            new InntektsmeldingDto.BortfaltNaturalytelse(
                naturalytelseAndreFraDato, naturalytelseAndreTilDato,
                NaturalytelseType.BOLIG, naturalytelseBeløp),
            new InntektsmeldingDto.BortfaltNaturalytelse(
                naturalytelseTredjeTilDato, Tid.TIDENES_ENDE,
                NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS, naturalytelseBeløp));

        var inntektsmeldingDto = lagStandardInntektsmeldingDtoBuilder()
            .medBortfaltNaturalytelsePerioder(naturalytelser)
            .build();

        var pdfData = InntektsmeldingPdfDataMapper.mapInntektsmeldingPdfData(inntektsmeldingDto, ARBEIDSGIVER_NAVN, personInfo, ARBEIDSGIVER_IDENT, ForespørselType.BESTILT_AV_FAGSYSTEM);

        assertThat(pdfData.ingenGjenopptattNaturalytelse()).isFalse();
        assertThat(pdfData.ingenBortfaltNaturalytelse()).isFalse();
        assertThat(pdfData.getNaturalytelser()).hasSize(5);

        var bortfalteNaturalytelser = pdfData.getNaturalytelser().stream().filter(NaturalYtelse::erBortfalt).toList();
        assertThat(bortfalteNaturalytelser).hasSize(3);

        var forventetFørsteFraDato = naturalytelseTilDato.plusDays(1);
        var forventetAndreFraDato = naturalytelseAndreTilDato.plusDays(1);

        var tilkomneNaturalytelser = pdfData.getNaturalytelser().stream().filter(naturalytelse -> !naturalytelse.erBortfalt()).toList();

        assertThat(tilkomneNaturalytelser).hasSize(2);
        assertThat(tilkomneNaturalytelser.getFirst().fom()).isEqualTo(formaterDatoForLister(forventetFørsteFraDato));
        assertThat(tilkomneNaturalytelser.getFirst().naturalytelseType()).isEqualTo("Bil");
        assertThat(tilkomneNaturalytelser.get(1).fom()).isEqualTo(formaterDatoForLister(forventetAndreFraDato));
        assertThat(tilkomneNaturalytelser.get(1).naturalytelseType()).isEqualTo("Bolig");
    }

    @Test
    void skal_mappe_pdfData_uten_naturalytser() {
        var inntektsmeldingDto = lagStandardInntektsmeldingDtoBuilder()
            .medBortfaltNaturalytelsePerioder(Collections.emptyList())
            .build();

        var pdfData = InntektsmeldingPdfDataMapper.mapInntektsmeldingPdfData(inntektsmeldingDto, ARBEIDSGIVER_NAVN, personInfo, ARBEIDSGIVER_IDENT, ForespørselType.BESTILT_AV_FAGSYSTEM);

        assertThat(pdfData.ingenGjenopptattNaturalytelse()).isTrue();
        assertThat(pdfData.ingenBortfaltNaturalytelse()).isTrue();
        assertThat(pdfData.getNaturalytelser()).isEmpty();
    }

    @Test
    void skal_mappe_pdfData_for_arbeidsgiverinitiert_nyansatt() {
        var refusjonsstartdato2 = LocalDate.now().plusWeeks(1);
        var refusjonsstartdato3 = refusjonsstartdato2.plusWeeks(2).plusDays(1);

        var refusjonsbeløp2 = BigDecimal.valueOf(34000);
        var refusjonsbeløp3 = BigDecimal.valueOf(32000);
        var refusjonsendringer = List.of(
            new InntektsmeldingDto.SøktRefusjon(refusjonsstartdato2, refusjonsbeløp2),
            new InntektsmeldingDto.SøktRefusjon(refusjonsstartdato3, refusjonsbeløp3));

        var inntektsmeldingDto = lagStandardInntektsmeldingDtoBuilder()
            .medMånedRefusjon(REFUSJON_BELØP)
            .medSøkteRefusjonsperioder(refusjonsendringer)
            .build();

        var pdfData = InntektsmeldingPdfDataMapper.mapInntektsmeldingPdfData(inntektsmeldingDto, ARBEIDSGIVER_NAVN, personInfo, ARBEIDSGIVER_IDENT, ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);

        assertThat(pdfData.ingenGjenopptattNaturalytelse()).isFalse();
        assertThat(pdfData.ingenBortfaltNaturalytelse()).isFalse();
        assertThat(pdfData.getMånedInntekt()).isNull();
        assertThat(pdfData.getEndringsarsaker()).isEmpty();
        assertThat(pdfData.getNaturalytelser()).isEmpty();
    }

    private InntektsmeldingDto.Builder lagStandardInntektsmeldingDtoBuilder() {
        return InntektsmeldingDto.builder()
            .medAktørId(new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(AKTØRID_SØKER.getAktørId()))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson(ORG_NUMMER, NAVN))
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medInntekt(INNTEKT)
            .medStartdato(START_DATO)
            .medMånedRefusjon(REFUSJON_BELØP)
            .medOpphørsdatoRefusjon(Tid.TIDENES_ENDE)
            .medInnsendtTidspunkt(OPPRETTETT_TIDSPUNKT)
            .medArbeidsgiver(new InntektsmeldingDto.Arbeidsgiver(ARBEIDSGIVER_IDENT))
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(List.of())
            .medEndringAvInntektÅrsaker(List.of());
    }
}
