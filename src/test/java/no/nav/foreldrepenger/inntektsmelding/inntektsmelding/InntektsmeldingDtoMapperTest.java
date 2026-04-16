package no.nav.foreldrepenger.inntektsmelding.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.EndringsårsakType;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.BortaltNaturalytelseEntitet;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.EndringsårsakEntitet;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.InntektsmeldingEntitet;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.KontaktpersonEntitet;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.RefusjonsendringEntitet;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;

class InntektsmeldingDtoMapperTest {

    private static final LocalDate START_DATO = LocalDate.of(2026, 3, 10);
    private static final BigDecimal MÅNED_INNTEKT = new BigDecimal("50000.00");
    private static final String ARBEIDSGIVER_IDENT = "999999999";
    private static final String AKTØR_ID = "1234567890123";

    @Test
    void skal_mappe_kontaktperson_fra_entitet() {
        var entitet = lagInntektsmeldingEntitetMedKontaktperson("Ola Nordmann", "12345678");

        var resultat = InntektsmeldingDtoMapper.mapKontaktperson(entitet);

        assertThat(resultat).isNotNull();
        assertThat(resultat.telefonnummer()).isEqualTo("12345678");
        assertThat(resultat.navn()).isEqualTo("Ola Nordmann");
    }

    @Test
    void skal_returnere_null_kontaktperson_når_entitet_mangler_kontaktperson() {
        var entitet = lagBasisInntektsmeldingEntitet().build();

        var resultat = InntektsmeldingDtoMapper.mapKontaktperson(entitet);

        assertThat(resultat).isNull();
    }

    @Test
    void skal_mappe_refusjonsendring_fra_entitet() {
        var refusjonsendring = new RefusjonsendringEntitet(START_DATO.plusMonths(1), new BigDecimal("30000.00"));

        var resultat = InntektsmeldingDtoMapper.mapRefusjonsendring(refusjonsendring);

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

        var resultat = InntektsmeldingDtoMapper.mapBortfaltNaturalytelse(bortfalt);

        assertThat(resultat.fom()).isEqualTo(START_DATO);
        assertThat(resultat.tom()).isEqualTo(START_DATO.plusMonths(3));
        assertThat(resultat.naturalytelsetype()).isEqualTo(NaturalytelseType.ELEKTRISK_KOMMUNIKASJON);
        assertThat(resultat.beløp()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void skal_mappe_endringsårsak_med_alle_felter() {
        var endringsårsak = EndringsårsakEntitet.builder()
            .medÅrsak(EndringsårsakType.TARIFFENDRING)
            .medFom(START_DATO)
            .medTom(START_DATO.plusMonths(1))
            .medBleKjentFra(START_DATO.minusDays(5))
            .build();

        var resultat = InntektsmeldingDtoMapper.mapEndringsårsak(endringsårsak);

        assertThat(resultat.årsak()).isEqualTo(EndringsårsakType.TARIFFENDRING);
        assertThat(resultat.fom()).isEqualTo(START_DATO);
        assertThat(resultat.tom()).isEqualTo(START_DATO.plusMonths(1));
        assertThat(resultat.bleKjentFom()).isEqualTo(START_DATO.minusDays(5));
    }

    @Test
    void skal_mappe_endringsårsak_med_kun_årsak() {
        var endringsårsak = EndringsårsakEntitet.builder()
            .medÅrsak(EndringsårsakType.BONUS)
            .build();

        var resultat = InntektsmeldingDtoMapper.mapEndringsårsak(endringsårsak);

        assertThat(resultat.årsak()).isEqualTo(EndringsårsakType.BONUS);
        assertThat(resultat.fom()).isNull();
        assertThat(resultat.tom()).isNull();
        assertThat(resultat.bleKjentFom()).isNull();
    }

    @Test
    void skal_mappe_alle_endringsårsaker_korrekt() {
        for (var kodeÅrsak : EndringsårsakType.values()) {
            var entitet = EndringsårsakEntitet.builder().medÅrsak(kodeÅrsak).build();
            var resultat = InntektsmeldingDtoMapper.mapEndringsårsak(entitet);
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
            var resultat = InntektsmeldingDtoMapper.mapBortfaltNaturalytelse(entitet);
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
                EndringsårsakEntitet.builder().medÅrsak(EndringsårsakType.FERIE).medFom(START_DATO).medTom(START_DATO.plusWeeks(2)).build(),
                EndringsårsakEntitet.builder().medÅrsak(EndringsårsakType.BONUS).build()
            ))
            .medOpprettetTidspunkt(opprettetTidspunkt)
            .build();

        // Map kontaktperson
        var kontaktperson = InntektsmeldingDtoMapper.mapKontaktperson(entitet);
        assertThat(kontaktperson).isNotNull();
        assertThat(kontaktperson.navn()).isEqualTo("Kari Nordmann");
        assertThat(kontaktperson.telefonnummer()).isEqualTo("87654321");

        // Map refusjonsendringer
        var refusjoner = entitet.getRefusjonsendringer().stream()
            .map(InntektsmeldingDtoMapper::mapRefusjonsendring)
            .toList();
        assertThat(refusjoner).hasSize(2);
        assertThat(refusjoner.getFirst().fom()).isEqualTo(START_DATO.plusMonths(1));
        assertThat(refusjoner.getFirst().beløp()).isEqualByComparingTo(new BigDecimal("35000.00"));
        assertThat(refusjoner.get(1).fom()).isEqualTo(START_DATO.plusMonths(2));
        assertThat(refusjoner.get(1).beløp()).isEqualByComparingTo(new BigDecimal("30000.00"));

        // Map bortfalte naturalytelser
        var naturalytelser = entitet.getBorfalteNaturalYtelser().stream()
            .map(InntektsmeldingDtoMapper::mapBortfaltNaturalytelse)
            .toList();
        assertThat(naturalytelser).hasSize(1);
        assertThat(naturalytelser.getFirst().naturalytelsetype()).isEqualTo(NaturalytelseType.BIL);
        assertThat(naturalytelser.getFirst().beløp()).isEqualByComparingTo(new BigDecimal("3000.00"));

        // Map endringsårsaker
        var endringsårsaker = entitet.getEndringsårsaker().stream()
            .map(InntektsmeldingDtoMapper::mapEndringsårsak)
            .toList();
        assertThat(endringsårsaker).hasSize(2);
        assertThat(endringsårsaker.getFirst().årsak()).isEqualTo(EndringsårsakType.FERIE);
        assertThat(endringsårsaker.getFirst().fom()).isEqualTo(START_DATO);
        assertThat(endringsårsaker.get(1).årsak()).isEqualTo(EndringsårsakType.BONUS);
        assertThat(endringsårsaker.get(1).fom()).isNull();
    }

    @Test
    void skal_mappe_entitet_uten_lister() {
        var entitet = lagBasisInntektsmeldingEntitet().build();

        var kontaktperson = InntektsmeldingDtoMapper.mapKontaktperson(entitet);
        var refusjoner = entitet.getRefusjonsendringer().stream()
            .map(InntektsmeldingDtoMapper::mapRefusjonsendring)
            .toList();
        var naturalytelser = entitet.getBorfalteNaturalYtelser().stream()
            .map(InntektsmeldingDtoMapper::mapBortfaltNaturalytelse)
            .toList();
        var endringsårsaker = entitet.getEndringsårsaker().stream()
            .map(InntektsmeldingDtoMapper::mapEndringsårsak)
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
        var ytelse = entitet.getYtelsetype();
        assertThat(ytelse).isEqualTo(Ytelsetype.FORELDREPENGER);
    }

    @Test
    void skal_mappe_ytelsetype_svangerskapspenger() {
        var entitet = lagBasisInntektsmeldingEntitetMedYtelse(Ytelsetype.SVANGERSKAPSPENGER).build();
        var ytelse = entitet.getYtelsetype();
        assertThat(ytelse).isEqualTo(Ytelsetype.SVANGERSKAPSPENGER);
    }

    @Test
    void skal_mappe_alle_ytelsetyper_korrekt() {
        for (var type : Ytelsetype.values()) {
            var ytelse = Ytelsetype.valueOf(type.name());
            assertThat(ytelse.name()).isEqualTo(type.name());
        }
    }

    @Test
    void skal_verifisere_builder_setter_alle_felter() {
        var uuid = java.util.UUID.randomUUID();
        var tidspunkt = LocalDateTime.now();

        var aktørId = new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId("1234567890123");
        var dto = InntektsmeldingDto.builder()
            .medInntektsmeldingUuid(uuid)
            .medAktørId(aktørId)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medArbeidsgiver(new Arbeidsgiver(ARBEIDSGIVER_IDENT))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("11111111", "Test Testesen"))
            .medStartdato(START_DATO)
            .medInntekt(MÅNED_INNTEKT)
            .medInnsendtTidspunkt(tidspunkt)
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medMånedRefusjon(new BigDecimal("30000.00"))
            .medOpphørsdatoRefusjon(START_DATO.plusMonths(12))
            .medAvsenderSystem(new InntektsmeldingDto.AvsenderSystem("TestSystem", "1.0"))
            .medSøkteRefusjonsperioder(List.of(new InntektsmeldingDto.SøktRefusjon(START_DATO, new BigDecimal("25000"))))
            .medBortfaltNaturalytelsePerioder(List.of(
                new InntektsmeldingDto.BortfaltNaturalytelse(START_DATO, START_DATO.plusMonths(1),
                    NaturalytelseType.LOSJI, new BigDecimal("1000"))))
            .medEndringAvInntektÅrsaker(List.of(
                new InntektsmeldingDto.Endringsårsak(
                    EndringsårsakType.NYANSATT,
                    null, null, null)))
            .build();

        assertThat(dto.getInntektsmeldingUuid()).isEqualTo(uuid);
        assertThat(dto.getAktørId()).isEqualTo(aktørId);
        assertThat(dto.getYtelse()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(dto.getArbeidsgiver().orgnr()).isEqualTo(ARBEIDSGIVER_IDENT);
        assertThat(dto.getKontaktperson().navn()).isEqualTo("Test Testesen");
        assertThat(dto.getKontaktperson().telefonnummer()).isEqualTo("11111111");
        assertThat(dto.getStartdato()).isEqualTo(START_DATO);
        assertThat(dto.getMånedInntekt()).isEqualByComparingTo(MÅNED_INNTEKT);
        assertThat(dto.getInnsendtTidspunkt()).isEqualTo(tidspunkt);
        assertThat(dto.getKildesystem()).isEqualTo(Kildesystem.LØNN_OG_PERSONAL_SYSTEM);
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
        assertThat(dto.getMånedInntekt()).isNull();
        assertThat(dto.getMånedRefusjon()).isNull();
        assertThat(dto.getOpphørsdatoRefusjon()).isNull();
        assertThat(dto.getInnsendtTidspunkt()).isNull();
        assertThat(dto.getAvsenderSystem()).isNull();
        assertThat(dto.getSøkteRefusjonsperioder()).isNull();
        assertThat(dto.getBortfaltNaturalytelsePerioder()).isNull();
        assertThat(dto.getEndringAvInntektÅrsaker()).isNull();
    }

    @Test
    void skal_mappe_komplett_entitet_til_dto_via_mapFraEntitet() {
        var opprettetTidspunkt = LocalDateTime.of(2026, 3, 10, 9, 0, 0);

        var entitet = lagBasisInntektsmeldingEntitet()
            .medKontaktperson(new KontaktpersonEntitet("Kari Nordmann", "87654321"))
            .medMånedRefusjon(new BigDecimal("40000.00"))
            .medRefusjonOpphørsdato(START_DATO.plusMonths(6))
            .medOpprettetAv("saksbehandler")
            .medRefusjonsendringer(List.of(
                new RefusjonsendringEntitet(START_DATO.plusMonths(1), new BigDecimal("35000.00"))))
            .medBortfaltNaturalytelser(List.of(
                BortaltNaturalytelseEntitet.builder()
                    .medPeriode(START_DATO, START_DATO.plusMonths(1))
                    .medType(NaturalytelseType.BIL)
                    .medMånedBeløp(new BigDecimal("3000.00"))
                    .build()))
            .medEndringsårsaker(List.of(
                EndringsårsakEntitet.builder().medÅrsak(EndringsårsakType.FERIE).medFom(START_DATO).medTom(START_DATO.plusWeeks(2)).build()))
            .medOpprettetTidspunkt(opprettetTidspunkt)
            .build();

        var dto = InntektsmeldingDtoMapper.mapFraEntitet(entitet);

        assertThat(dto.getAktørId().getAktørId()).isEqualTo(AKTØR_ID);
        assertThat(dto.getYtelse()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(dto.getArbeidsgiver().orgnr()).isEqualTo(ARBEIDSGIVER_IDENT);
        assertThat(dto.getStartdato()).isEqualTo(START_DATO);
        assertThat(dto.getMånedInntekt()).isEqualByComparingTo(MÅNED_INNTEKT);
        assertThat(dto.getMånedRefusjon()).isEqualByComparingTo(new BigDecimal("40000.00"));
        assertThat(dto.getOpphørsdatoRefusjon()).isEqualTo(START_DATO.plusMonths(6));
        assertThat(dto.getOpprettetAv()).isEqualTo("saksbehandler");
        assertThat(dto.getKildesystem()).isEqualTo(Kildesystem.ARBEIDSGIVERPORTAL);
        assertThat(dto.getInnsendtTidspunkt()).isEqualTo(opprettetTidspunkt);
        assertThat(dto.getInntektsmeldingUuid()).isNotNull();
        assertThat(dto.getKontaktperson().navn()).isEqualTo("Kari Nordmann");
        assertThat(dto.getKontaktperson().telefonnummer()).isEqualTo("87654321");
        assertThat(dto.getSøkteRefusjonsperioder()).hasSize(1);
        assertThat(dto.getSøkteRefusjonsperioder().getFirst().fom()).isEqualTo(START_DATO.plusMonths(1));
        assertThat(dto.getBortfaltNaturalytelsePerioder()).hasSize(1);
        assertThat(dto.getBortfaltNaturalytelsePerioder().getFirst().naturalytelsetype()).isEqualTo(NaturalytelseType.BIL);
        assertThat(dto.getEndringAvInntektÅrsaker()).hasSize(1);
        assertThat(dto.getEndringAvInntektÅrsaker().getFirst().årsak()).isEqualTo(EndringsårsakType.FERIE);
    }

    @Test
    void skal_mappe_entitet_uten_valgfrie_felter_via_mapFraEntitet() {
        var entitet = lagBasisInntektsmeldingEntitet().build();

        var dto = InntektsmeldingDtoMapper.mapFraEntitet(entitet);

        assertThat(dto.getKontaktperson()).isNull();
        assertThat(dto.getMånedRefusjon()).isNull();
        assertThat(dto.getOpphørsdatoRefusjon()).isNull();
        assertThat(dto.getSøkteRefusjonsperioder()).isEmpty();
        assertThat(dto.getBortfaltNaturalytelsePerioder()).isEmpty();
        assertThat(dto.getEndringAvInntektÅrsaker()).isEmpty();
    }

    @Test
    void skal_mappe_dto_til_entitet_via_mapTilEntitet() {
        var aktørId = new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(AKTØR_ID);
        var dto = InntektsmeldingDto.builder()
            .medAktørId(aktørId)
            .medYtelse(Ytelsetype.SVANGERSKAPSPENGER)
            .medArbeidsgiver(new Arbeidsgiver(ARBEIDSGIVER_IDENT))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("99887766", "Per Hansen"))
            .medStartdato(START_DATO)
            .medInntekt(new BigDecimal("55000"))
            .medMånedRefusjon(new BigDecimal("25000"))
            .medOpphørsdatoRefusjon(START_DATO.plusMonths(6))
            .medOpprettetAv("bruker")
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medSøkteRefusjonsperioder(List.of(
                new InntektsmeldingDto.SøktRefusjon(START_DATO.plusMonths(2), new BigDecimal("20000"))))
            .medBortfaltNaturalytelsePerioder(List.of(
                new InntektsmeldingDto.BortfaltNaturalytelse(START_DATO, START_DATO.plusMonths(3), NaturalytelseType.LOSJI, new BigDecimal("2000"))))
            .medEndringAvInntektÅrsaker(List.of(
                new InntektsmeldingDto.Endringsårsak(EndringsårsakType.VARIG_LØNNSENDRING, START_DATO.minusMonths(1), null, START_DATO.minusWeeks(2))))
            .build();

        var entitet = InntektsmeldingDtoMapper.mapTilEntitet(dto);

        assertThat(entitet.getAktørId().getAktørId()).isEqualTo(AKTØR_ID);
        assertThat(entitet.getYtelsetype()).isEqualTo(Ytelsetype.SVANGERSKAPSPENGER);
        assertThat(entitet.getArbeidsgiverIdent()).isEqualTo(ARBEIDSGIVER_IDENT);
        assertThat(entitet.getStartDato()).isEqualTo(START_DATO);
        assertThat(entitet.getMånedInntekt()).isEqualByComparingTo(new BigDecimal("55000"));
        assertThat(entitet.getMånedRefusjon()).isEqualByComparingTo(new BigDecimal("25000"));
        assertThat(entitet.getOpphørsdatoRefusjon()).isEqualTo(START_DATO.plusMonths(6));
        assertThat(entitet.getOpprettetAv()).isEqualTo("bruker");
        assertThat(entitet.getKildesystem()).isEqualTo(Kildesystem.LØNN_OG_PERSONAL_SYSTEM);
        assertThat(entitet.getKontaktperson().getNavn()).isEqualTo("Per Hansen");
        assertThat(entitet.getKontaktperson().getTelefonnummer()).isEqualTo("99887766");
        assertThat(entitet.getRefusjonsendringer()).hasSize(1);
        assertThat(entitet.getBorfalteNaturalYtelser()).hasSize(1);
        assertThat(entitet.getEndringsårsaker()).hasSize(1);
    }

    @Test
    void skal_filtrere_bort_refusjonsendring_på_startdato_ved_mapping_til_entitet() {
        var aktørId = new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(AKTØR_ID);
        var opphørsdato = START_DATO.plusMonths(6);
        var dto = InntektsmeldingDto.builder()
            .medAktørId(aktørId)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medArbeidsgiver(new Arbeidsgiver(ARBEIDSGIVER_IDENT))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("12345678", "Test"))
            .medStartdato(START_DATO)
            .medInntekt(MÅNED_INNTEKT)
            .medMånedRefusjon(new BigDecimal("30000"))
            .medOpphørsdatoRefusjon(opphørsdato)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medSøkteRefusjonsperioder(List.of(
                new InntektsmeldingDto.SøktRefusjon(START_DATO, new BigDecimal("30000")),                   // filtreres bort (startdato)
                new InntektsmeldingDto.SøktRefusjon(opphørsdato.plusDays(1), BigDecimal.ZERO),               // filtreres bort (opphørsdato + 1)
                new InntektsmeldingDto.SøktRefusjon(START_DATO.plusMonths(2), new BigDecimal("20000"))))     // beholdes
            .medBortfaltNaturalytelsePerioder(List.of())
            .medEndringAvInntektÅrsaker(List.of())
            .build();

        var entitet = InntektsmeldingDtoMapper.mapTilEntitet(dto);

        assertThat(entitet.getRefusjonsendringer()).hasSize(1);
        assertThat(entitet.getRefusjonsendringer().getFirst().getFom()).isEqualTo(START_DATO.plusMonths(2));
        assertThat(entitet.getRefusjonsendringer().getFirst().getRefusjonPrMnd()).isEqualByComparingTo(new BigDecimal("20000"));
    }

    @Test
    void skal_roundtrippe_grunnleggende_felter_via_dto_til_entitet_til_dto() {
        var aktørId = new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(AKTØR_ID);
        var original = InntektsmeldingDto.builder()
            .medAktørId(aktørId)
            .medYtelse(Ytelsetype.SVANGERSKAPSPENGER)
            .medArbeidsgiver(new Arbeidsgiver(ARBEIDSGIVER_IDENT))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("11112222", "Kari Nordmann"))
            .medStartdato(START_DATO)
            .medInntekt(new BigDecimal("60000"))
            .medOpprettetAv("bruker")
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(List.of())
            .medEndringAvInntektÅrsaker(List.of())
            .build();

        var entitet = InntektsmeldingDtoMapper.mapTilEntitet(original);
        var roundtripped = InntektsmeldingDtoMapper.mapFraEntitet(entitet);

        assertThat(roundtripped.getAktørId()).isEqualTo(original.getAktørId());
        assertThat(roundtripped.getYtelse()).isEqualTo(original.getYtelse());
        assertThat(roundtripped.getArbeidsgiver()).isEqualTo(original.getArbeidsgiver());
        assertThat(roundtripped.getStartdato()).isEqualTo(original.getStartdato());
        assertThat(roundtripped.getMånedInntekt()).isEqualByComparingTo(original.getMånedInntekt());
        assertThat(roundtripped.getKildesystem()).isEqualTo(original.getKildesystem());
        assertThat(roundtripped.getOpprettetAv()).isEqualTo(original.getOpprettetAv());
        assertThat(roundtripped.getKontaktperson().navn()).isEqualTo(original.getKontaktperson().navn());
        assertThat(roundtripped.getKontaktperson().telefonnummer()).isEqualTo(original.getKontaktperson().telefonnummer());
    }

    @Test
    void skal_roundtrippe_endringsårsaker() {
        var aktørId = new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(AKTØR_ID);
        var original = InntektsmeldingDto.builder()
            .medAktørId(aktørId)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medArbeidsgiver(new Arbeidsgiver(ARBEIDSGIVER_IDENT))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("12345678", "Test"))
            .medStartdato(START_DATO)
            .medInntekt(MÅNED_INNTEKT)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(List.of())
            .medEndringAvInntektÅrsaker(List.of(
                new InntektsmeldingDto.Endringsårsak(EndringsårsakType.TARIFFENDRING, START_DATO, START_DATO.plusMonths(1), START_DATO.minusDays(5)),
                new InntektsmeldingDto.Endringsårsak(EndringsårsakType.BONUS, null, null, null)))
            .build();

        var entitet = InntektsmeldingDtoMapper.mapTilEntitet(original);
        var roundtripped = InntektsmeldingDtoMapper.mapFraEntitet(entitet);

        assertThat(roundtripped.getEndringAvInntektÅrsaker()).hasSize(2);
        assertThat(roundtripped.getEndringAvInntektÅrsaker().getFirst().årsak()).isEqualTo(EndringsårsakType.TARIFFENDRING);
        assertThat(roundtripped.getEndringAvInntektÅrsaker().getFirst().fom()).isEqualTo(START_DATO);
        assertThat(roundtripped.getEndringAvInntektÅrsaker().getFirst().tom()).isEqualTo(START_DATO.plusMonths(1));
        assertThat(roundtripped.getEndringAvInntektÅrsaker().getFirst().bleKjentFom()).isEqualTo(START_DATO.minusDays(5));
        assertThat(roundtripped.getEndringAvInntektÅrsaker().get(1).årsak()).isEqualTo(EndringsårsakType.BONUS);
        assertThat(roundtripped.getEndringAvInntektÅrsaker().get(1).fom()).isNull();
    }

    @Test
    void skal_roundtrippe_bortfalt_naturalytelse() {
        var aktørId = new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(AKTØR_ID);
        var original = InntektsmeldingDto.builder()
            .medAktørId(aktørId)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medArbeidsgiver(new Arbeidsgiver(ARBEIDSGIVER_IDENT))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("12345678", "Test"))
            .medStartdato(START_DATO)
            .medInntekt(MÅNED_INNTEKT)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(List.of(
                new InntektsmeldingDto.BortfaltNaturalytelse(START_DATO, START_DATO.plusMonths(6), NaturalytelseType.ELEKTRISK_KOMMUNIKASJON, new BigDecimal("500")),
                new InntektsmeldingDto.BortfaltNaturalytelse(START_DATO, START_DATO.plusMonths(3), NaturalytelseType.LOSJI, new BigDecimal("2000"))))
            .medEndringAvInntektÅrsaker(List.of())
            .build();

        var entitet = InntektsmeldingDtoMapper.mapTilEntitet(original);
        var roundtripped = InntektsmeldingDtoMapper.mapFraEntitet(entitet);

        assertThat(roundtripped.getBortfaltNaturalytelsePerioder()).hasSize(2);
        assertThat(roundtripped.getBortfaltNaturalytelsePerioder().getFirst().naturalytelsetype()).isEqualTo(NaturalytelseType.ELEKTRISK_KOMMUNIKASJON);
        assertThat(roundtripped.getBortfaltNaturalytelsePerioder().getFirst().beløp()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(roundtripped.getBortfaltNaturalytelsePerioder().get(1).naturalytelsetype()).isEqualTo(NaturalytelseType.LOSJI);
    }

    @Test
    void skal_mappe_lpssystem_informasjon() {
        var aktørId = new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(AKTØR_ID);
        var original = InntektsmeldingDto.builder()
            .medAktørId(aktørId)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medArbeidsgiver(Arbeidsgiver.fra(ARBEIDSGIVER_IDENT))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("12345678", "Test"))
            .medStartdato(START_DATO)
            .medInntekt(MÅNED_INNTEKT)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(List.of())
            .medEndringAvInntektÅrsaker(List.of())
            .medAvsenderSystem(new InntektsmeldingDto.AvsenderSystem("TestSystem", "1.0"))
            .build();

        var entitet = InntektsmeldingDtoMapper.mapTilEntitet(original);
        var roundtripped = InntektsmeldingDtoMapper.mapFraEntitet(entitet);

        assertThat(roundtripped.getAvsenderSystem()).isNotNull();
        assertThat(roundtripped.getAvsenderSystem().navn()).isNotNull().isEqualTo("TestSystem");
        assertThat(roundtripped.getAvsenderSystem().versjon()).isEqualTo("1.0");
    }

    @Test
    void skal_mappe_dto_til_entitet_uten_kontaktperson() {
        var aktørId = new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(AKTØR_ID);
        var dto = InntektsmeldingDto.builder()
            .medAktørId(aktørId)
            .medYtelse(Ytelsetype.SVANGERSKAPSPENGER)
            .medArbeidsgiver(new Arbeidsgiver(ARBEIDSGIVER_IDENT))
            .medStartdato(START_DATO)
            .medInntekt(new BigDecimal("55000"))
            .medMånedRefusjon(new BigDecimal("25000"))
            .medOpphørsdatoRefusjon(START_DATO.plusMonths(6))
            .medOpprettetAv("bruker")
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(List.of())
            .medEndringAvInntektÅrsaker(List.of())
            .build();

        var entitet = InntektsmeldingDtoMapper.mapTilEntitet(dto);

        assertThat(entitet.getAktørId().getAktørId()).isEqualTo(AKTØR_ID);
        assertThat(entitet.getKontaktperson()).isNull();
    }

    private InntektsmeldingEntitet.Builder lagBasisInntektsmeldingEntitet() {
        return lagBasisInntektsmeldingEntitetMedYtelse(Ytelsetype.FORELDREPENGER);
    }

    private InntektsmeldingEntitet.Builder lagBasisInntektsmeldingEntitetMedYtelse(Ytelsetype ytelsetype) {
        return InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet(AKTØR_ID))
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
