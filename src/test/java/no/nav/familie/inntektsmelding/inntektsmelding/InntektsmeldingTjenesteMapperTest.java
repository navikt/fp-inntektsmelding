package no.nav.familie.inntektsmelding.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.koder.Endringsårsak;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørId;

class InntektsmeldingTjenesteMapperTest {

    private static final LocalDate START_DATO = LocalDate.of(2026, 3, 10);
    private static final BigDecimal MÅNED_INNTEKT = new BigDecimal("50000.00");
    private static final String ARBEIDSGIVER_IDENT = "999999999";
    private static final String AKTØR_ID = "1234567890123";

    @Test
    void skal_mappe_kontaktperson_fra_entitet() {
        var entitet = lagInntektsmeldingEntitetMedKontaktperson("Ola Nordmann", "12345678");

        var resultat = InntektsmeldingTjeneste.mapKontaktperson(entitet);

        assertThat(resultat).isNotNull();
        assertThat(resultat.telefonnummer()).isEqualTo("12345678");
        assertThat(resultat.navn()).isEqualTo("Ola Nordmann");
    }

    @Test
    void skal_returnere_null_kontaktperson_når_entitet_mangler_kontaktperson() {
        var entitet = lagBasisInntektsmeldingEntitet().build();

        var resultat = InntektsmeldingTjeneste.mapKontaktperson(entitet);

        assertThat(resultat).isNull();
    }

    @Test
    void skal_mappe_refusjonsendring_fra_entitet() {
        var refusjonsendring = new RefusjonsendringEntitet(START_DATO.plusMonths(1), new BigDecimal("30000.00"));

        var resultat = InntektsmeldingTjeneste.mapRefusjonsendring(refusjonsendring);

        assertThat(resultat.fom()).isEqualTo(START_DATO.plusMonths(1));
        assertThat(resultat.beløp()).isEqualByComparingTo(new BigDecimal("30000.00"));
    }

    @Test
    void skal_mappe_bortfalt_naturalytelse_fra_entitet() {
        var bortfalt = BortaltNaturalytelseEntitet.builder()
            .medPeriode(START_DATO, START_DATO.plusMonths(3))
            .medType(NaturalytelseType.ELEKTRISK_KOMMUNIKASJON)
            .medMånedBeløp(new BigDecimal("500.00"))
            .build();

        var resultat = InntektsmeldingTjeneste.mapBortfaltNaturalytelse(bortfalt);

        assertThat(resultat.fom()).isEqualTo(START_DATO);
        assertThat(resultat.tom()).isEqualTo(START_DATO.plusMonths(3));
        assertThat(resultat.naturalytelsetype()).isEqualTo(InntektsmeldingDto.Naturalytelsetype.ELEKTRISK_KOMMUNIKASJON);
        assertThat(resultat.beløp()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void skal_mappe_endringsårsak_med_alle_felter() {
        var endringsårsak = EndringsårsakEntitet.builder()
            .medÅrsak(Endringsårsak.TARIFFENDRING)
            .medFom(START_DATO)
            .medTom(START_DATO.plusMonths(1))
            .medBleKjentFra(START_DATO.minusDays(5))
            .build();

        var resultat = InntektsmeldingTjeneste.mapEndringsårsak(endringsårsak);

        assertThat(resultat.årsak()).isEqualTo(no.nav.familie.inntektsmelding.inntektsmelding.rest.kontrakt.Endringsårsak.TARIFFENDRING);
        assertThat(resultat.fom()).isEqualTo(START_DATO);
        assertThat(resultat.tom()).isEqualTo(START_DATO.plusMonths(1));
        assertThat(resultat.bleKjentFom()).isEqualTo(START_DATO.minusDays(5));
    }

    @Test
    void skal_mappe_endringsårsak_med_kun_årsak() {
        var endringsårsak = EndringsårsakEntitet.builder()
            .medÅrsak(Endringsårsak.BONUS)
            .build();

        var resultat = InntektsmeldingTjeneste.mapEndringsårsak(endringsårsak);

        assertThat(resultat.årsak()).isEqualTo(no.nav.familie.inntektsmelding.inntektsmelding.rest.kontrakt.Endringsårsak.BONUS);
        assertThat(resultat.fom()).isNull();
        assertThat(resultat.tom()).isNull();
        assertThat(resultat.bleKjentFom()).isNull();
    }

    @Test
    void skal_mappe_alle_endringsårsaker_korrekt() {
        for (var kodeÅrsak : Endringsårsak.values()) {
            var entitet = EndringsårsakEntitet.builder().medÅrsak(kodeÅrsak).build();
            var resultat = InntektsmeldingTjeneste.mapEndringsårsak(entitet);
            assertThat(resultat.årsak().name()).isEqualTo(kodeÅrsak.name());
        }
    }

    @Test
    void skal_mappe_alle_naturalytelsetyper_korrekt() {
        for (var type : NaturalytelseType.values()) {
            var entitet = BortaltNaturalytelseEntitet.builder()
                .medPeriode(START_DATO, START_DATO.plusDays(1))
                .medType(type)
                .medMånedBeløp(new BigDecimal("100"))
                .build();
            var resultat = InntektsmeldingTjeneste.mapBortfaltNaturalytelse(entitet);
            assertThat(resultat.naturalytelsetype().name()).isEqualTo(type.name());
        }
    }

    @Test
    void skal_mappe_komplett_inntektsmelding_entitet() {
        var opprettetTidspunkt = LocalDateTime.of(2026, 3, 10, 9, 0, 0);

        var entitet = lagBasisInntektsmeldingEntitet()
            .medKontaktperson(new KontaktpersonEntitet("Kari Nordmann", "87654321"))
            .medMånedRefusjon(new BigDecimal("40000.00"))
            .medRefusjonOpphørsdato(START_DATO.plusMonths(6))
            .medRefusjonsendringer(List.of(
                new RefusjonsendringEntitet(START_DATO.plusMonths(1), new BigDecimal("35000.00")),
                new RefusjonsendringEntitet(START_DATO.plusMonths(2), new BigDecimal("30000.00"))
            ))
            .medBortfaltNaturalytelser(List.of(
                BortaltNaturalytelseEntitet.builder()
                    .medPeriode(START_DATO, START_DATO.plusMonths(1))
                    .medType(NaturalytelseType.BIL)
                    .medMånedBeløp(new BigDecimal("3000.00"))
                    .build()
            ))
            .medEndringsårsaker(List.of(
                EndringsårsakEntitet.builder().medÅrsak(Endringsårsak.FERIE).medFom(START_DATO).medTom(START_DATO.plusWeeks(2)).build(),
                EndringsårsakEntitet.builder().medÅrsak(Endringsårsak.BONUS).build()
            ))
            .medOpprettetTidspunkt(opprettetTidspunkt)
            .build();

        // Map kontaktperson
        var kontaktperson = InntektsmeldingTjeneste.mapKontaktperson(entitet);
        assertThat(kontaktperson).isNotNull();
        assertThat(kontaktperson.navn()).isEqualTo("Kari Nordmann");
        assertThat(kontaktperson.telefonnummer()).isEqualTo("87654321");

        // Map refusjonsendringer
        var refusjoner = entitet.getRefusjonsendringer().stream()
            .map(InntektsmeldingTjeneste::mapRefusjonsendring)
            .toList();
        assertThat(refusjoner).hasSize(2);
        assertThat(refusjoner.get(0).fom()).isEqualTo(START_DATO.plusMonths(1));
        assertThat(refusjoner.get(0).beløp()).isEqualByComparingTo(new BigDecimal("35000.00"));
        assertThat(refusjoner.get(1).fom()).isEqualTo(START_DATO.plusMonths(2));
        assertThat(refusjoner.get(1).beløp()).isEqualByComparingTo(new BigDecimal("30000.00"));

        // Map bortfalte naturalytelser
        var naturalytelser = entitet.getBorfalteNaturalYtelser().stream()
            .map(InntektsmeldingTjeneste::mapBortfaltNaturalytelse)
            .toList();
        assertThat(naturalytelser).hasSize(1);
        assertThat(naturalytelser.getFirst().naturalytelsetype()).isEqualTo(InntektsmeldingDto.Naturalytelsetype.BIL);
        assertThat(naturalytelser.getFirst().beløp()).isEqualByComparingTo(new BigDecimal("3000.00"));

        // Map endringsårsaker
        var endringsårsaker = entitet.getEndringsårsaker().stream()
            .map(InntektsmeldingTjeneste::mapEndringsårsak)
            .toList();
        assertThat(endringsårsaker).hasSize(2);
        assertThat(endringsårsaker.get(0).årsak()).isEqualTo(no.nav.familie.inntektsmelding.inntektsmelding.rest.kontrakt.Endringsårsak.FERIE);
        assertThat(endringsårsaker.get(0).fom()).isEqualTo(START_DATO);
        assertThat(endringsårsaker.get(1).årsak()).isEqualTo(no.nav.familie.inntektsmelding.inntektsmelding.rest.kontrakt.Endringsårsak.BONUS);
        assertThat(endringsårsaker.get(1).fom()).isNull();
    }

    @Test
    void skal_mappe_entitet_uten_lister() {
        var entitet = lagBasisInntektsmeldingEntitet().build();

        var kontaktperson = InntektsmeldingTjeneste.mapKontaktperson(entitet);
        var refusjoner = entitet.getRefusjonsendringer().stream()
            .map(InntektsmeldingTjeneste::mapRefusjonsendring)
            .toList();
        var naturalytelser = entitet.getBorfalteNaturalYtelser().stream()
            .map(InntektsmeldingTjeneste::mapBortfaltNaturalytelse)
            .toList();
        var endringsårsaker = entitet.getEndringsårsaker().stream()
            .map(InntektsmeldingTjeneste::mapEndringsårsak)
            .toList();

        assertThat(kontaktperson).isNull();
        assertThat(refusjoner).isEmpty();
        assertThat(naturalytelser).isEmpty();
        assertThat(endringsårsaker).isEmpty();
    }

    @Test
    void skal_mappe_månedRefusjon_og_opphørsdato_fra_entitet() {
        var månedRefusjon = new BigDecimal("35000.00");
        var opphørsdato = START_DATO.plusMonths(6);

        var entitet = lagBasisInntektsmeldingEntitet()
            .medMånedRefusjon(månedRefusjon)
            .medRefusjonOpphørsdato(opphørsdato)
            .build();

        assertThat(entitet.getMånedRefusjon()).isEqualByComparingTo(månedRefusjon);
        assertThat(entitet.getOpphørsdatoRefusjon()).isEqualTo(opphørsdato);
    }

    @Test
    void skal_mappe_null_månedRefusjon_og_opphørsdato_når_ikke_satt() {
        var entitet = lagBasisInntektsmeldingEntitet().build();

        assertThat(entitet.getMånedRefusjon()).isNull();
        assertThat(entitet.getOpphørsdatoRefusjon()).isNull();
    }

    @Test
    void skal_mappe_ytelsetype_foreldrepenger() {
        var entitet = lagBasisInntektsmeldingEntitet().build();
        var ytelse = InntektsmeldingDto.Ytelse.valueOf(entitet.getYtelsetype().name());
        assertThat(ytelse).isEqualTo(InntektsmeldingDto.Ytelse.FORELDREPENGER);
    }

    @Test
    void skal_mappe_ytelsetype_svangerskapspenger() {
        var entitet = lagBasisInntektsmeldingEntitetMedYtelse(Ytelsetype.SVANGERSKAPSPENGER).build();
        var ytelse = InntektsmeldingDto.Ytelse.valueOf(entitet.getYtelsetype().name());
        assertThat(ytelse).isEqualTo(InntektsmeldingDto.Ytelse.SVANGERSKAPSPENGER);
    }

    @Test
    void skal_mappe_alle_ytelsetyper_korrekt() {
        for (var type : Ytelsetype.values()) {
            var ytelse = InntektsmeldingDto.Ytelse.valueOf(type.name());
            assertThat(ytelse.name()).isEqualTo(type.name());
        }
    }

    @Test
    void skal_verifisere_builder_setter_alle_felter() {
        var uuid = java.util.UUID.randomUUID();
        var forespørselUuid = java.util.UUID.randomUUID();
        var tidspunkt = LocalDateTime.now();

        var dto = InntektsmeldingDto.builder()
            .medInntektsmeldingUuid(uuid)
            .medAktørId("12345678901")
            .medYtelse(InntektsmeldingDto.Ytelse.FORELDREPENGER)
            .medArbeidsgiver(new InntektsmeldingDto.Arbeidsgiver(ARBEIDSGIVER_IDENT))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("11111111", "Test Testesen"))
            .medStartdato(START_DATO)
            .medInntekt(MÅNED_INNTEKT)
            .medInnsendingsårsak(InntektsmeldingDto.Innsendingsårsak.NY)
            .medInnsendingstype(InntektsmeldingDto.Innsendingstype.FORESPURT)
            .medInnsendtTidspunkt(tidspunkt)
            .medKildesystem(InntektsmeldingDto.Kildesystem.LPS_API)
            .medMånedRefusjon(new BigDecimal("30000.00"))
            .medOpphørsdatoRefusjon(START_DATO.plusMonths(12))
            .medAvsenderSystem(new InntektsmeldingDto.AvsenderSystem("TestSystem", "1.0"))
            .medSøkteRefusjonsperioder(List.of(new InntektsmeldingDto.SøktRefusjon(START_DATO, new BigDecimal("25000"))))
            .medBortfaltNaturalytelsePerioder(List.of(
                new InntektsmeldingDto.BortfaltNaturalytelse(START_DATO, START_DATO.plusMonths(1),
                    InntektsmeldingDto.Naturalytelsetype.LOSJI, new BigDecimal("1000"))))
            .medEndringAvInntektÅrsaker(List.of(
                new InntektsmeldingDto.Endringsårsaker(
                    no.nav.familie.inntektsmelding.inntektsmelding.rest.kontrakt.Endringsårsak.NYANSATT,
                    null, null, null)))
            .build();

        assertThat(dto.getInntektsmeldingUuid()).isEqualTo(uuid);
        assertThat(dto.getAktørId()).isEqualTo("12345678901");
        assertThat(dto.getYtelse()).isEqualTo(InntektsmeldingDto.Ytelse.FORELDREPENGER);
        assertThat(dto.getArbeidsgiver().ident()).isEqualTo(ARBEIDSGIVER_IDENT);
        assertThat(dto.getKontaktperson().navn()).isEqualTo("Test Testesen");
        assertThat(dto.getKontaktperson().telefonnummer()).isEqualTo("11111111");
        assertThat(dto.getStartdato()).isEqualTo(START_DATO);
        assertThat(dto.getInntekt()).isEqualByComparingTo(MÅNED_INNTEKT);
        assertThat(dto.getInnsendingsårsak()).isEqualTo(InntektsmeldingDto.Innsendingsårsak.NY);
        assertThat(dto.getInnsendingstype()).isEqualTo(InntektsmeldingDto.Innsendingstype.FORESPURT);
        assertThat(dto.getInnsendtTidspunkt()).isEqualTo(tidspunkt);
        assertThat(dto.getKildesystem()).isEqualTo(InntektsmeldingDto.Kildesystem.LPS_API);
        assertThat(dto.getMånedRefusjon()).isEqualByComparingTo(new BigDecimal("30000.00"));
        assertThat(dto.getOpphørsdatoRefusjon()).isEqualTo(START_DATO.plusMonths(12));
        assertThat(dto.getAvsenderSystem().navn()).isEqualTo("TestSystem");
        assertThat(dto.getAvsenderSystem().versjon()).isEqualTo("1.0");
        assertThat(dto.getSøkteRefusjonsperioder()).hasSize(1);
        assertThat(dto.getBortfaltNaturalytelsePerioder()).hasSize(1);
        assertThat(dto.getEndringAvInntektÅrsaker()).hasSize(1);
    }

    @Test
    void skal_bygge_dto_med_tomme_verdier() {
        var dto = InntektsmeldingDto.builder().build();

        assertThat(dto.getInntektsmeldingUuid()).isNull();
        assertThat(dto.getAktørId()).isNull();
        assertThat(dto.getYtelse()).isNull();
        assertThat(dto.getArbeidsgiver()).isNull();
        assertThat(dto.getKontaktperson()).isNull();
        assertThat(dto.getStartdato()).isNull();
        assertThat(dto.getInntekt()).isNull();
        assertThat(dto.getMånedRefusjon()).isNull();
        assertThat(dto.getOpphørsdatoRefusjon()).isNull();
        assertThat(dto.getInnsendingsårsak()).isNull();
        assertThat(dto.getInnsendingstype()).isNull();
        assertThat(dto.getInnsendtTidspunkt()).isNull();
        assertThat(dto.getAvsenderSystem()).isNull();
        assertThat(dto.getSøkteRefusjonsperioder()).isNull();
        assertThat(dto.getBortfaltNaturalytelsePerioder()).isNull();
        assertThat(dto.getEndringAvInntektÅrsaker()).isNull();
    }

    private InntektsmeldingEntitet.Builder lagBasisInntektsmeldingEntitet() {
        return lagBasisInntektsmeldingEntitetMedYtelse(Ytelsetype.FORELDREPENGER);
    }

    private InntektsmeldingEntitet.Builder lagBasisInntektsmeldingEntitetMedYtelse(Ytelsetype ytelsetype) {
        return InntektsmeldingEntitet.builder()
            .medAktørId(new AktørId(AKTØR_ID))
            .medYtelsetype(ytelsetype)
            .medArbeidsgiverIdent(ARBEIDSGIVER_IDENT)
            .medStartDato(START_DATO)
            .medMånedInntekt(MÅNED_INNTEKT)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL);
    }

    private InntektsmeldingEntitet lagInntektsmeldingEntitetMedKontaktperson(String navn, String telefonnummer) {
        return lagBasisInntektsmeldingEntitet()
            .medKontaktperson(new KontaktpersonEntitet(navn, telefonnummer))
            .build();
    }
}

