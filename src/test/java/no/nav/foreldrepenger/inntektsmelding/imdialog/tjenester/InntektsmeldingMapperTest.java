package no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDtoMapper;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;

import no.nav.foreldrepenger.inntektsmelding.typer.dto.ArbeidsgiverinitiertÅrsakDto;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Endringsårsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.EndringsårsakDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.NaturalytelsetypeDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;

class InntektsmeldingMapperTest {

    // ---- Tests for mapTilDto (SendInntektsmeldingRequestDto -> InntektsmeldingDto) ----

    @Test
    void skal_teste_dto_mapping_uten_ref_og_naturalytelse() {
        // Arrange
        var request = new SendInntektsmeldingRequestDto(UUID.randomUUID(), new AktørIdDto("9999999999999"), YtelseTypeDto.FORELDREPENGER,
            new ArbeidsgiverDto("999999999"), new SendInntektsmeldingRequestDto.KontaktpersonRequestDto("Testy test", "999999999"), LocalDate.now(),
            BigDecimal.valueOf(5000), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null);

        // Act
        var dto = InntektsmeldingMapper.mapTilDto(request);

        // Assert
        assertThat(dto.getAktørId().getAktørId()).isEqualTo(request.aktorId().id());
        assertThat(dto.getArbeidsgiver().orgnr()).isEqualTo(request.arbeidsgiverIdent().ident());
        assertThat(dto.getMånedInntekt()).isEqualByComparingTo(request.inntekt());
        assertThat(dto.getStartdato()).isEqualTo(request.startdato());
        assertThat(dto.getYtelse()).isEqualTo(KodeverkMapper.mapYtelsetype(request.ytelse()));
        assertThat(dto.getKontaktperson().navn()).isEqualTo(request.kontaktperson().navn());
        assertThat(dto.getKontaktperson().telefonnummer()).isEqualTo(request.kontaktperson().telefonnummer());
        assertThat(dto.getBortfaltNaturalytelsePerioder()).isEmpty();
        assertThat(dto.getMånedRefusjon()).isNull();
        assertThat(dto.getOpphørsdatoRefusjon()).isNull();
        assertThat(dto.getKildesystem()).isEqualTo(no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem.ARBEIDSGIVERPORTAL);
    }

    @Test
    void skal_teste_dto_mapping_med_ref_opphør() {
        // Arrange
        var request = new SendInntektsmeldingRequestDto(UUID.randomUUID(),
            new AktørIdDto("9999999999999"),
            YtelseTypeDto.FORELDREPENGER,
            new ArbeidsgiverDto("999999999"),
            new SendInntektsmeldingRequestDto.KontaktpersonRequestDto("Testy test", "999999999"),
            LocalDate.now(),
            BigDecimal.valueOf(5000),
            Arrays.asList(new SendInntektsmeldingRequestDto.Refusjon(LocalDate.now(), BigDecimal.valueOf(5000)),
                new SendInntektsmeldingRequestDto.Refusjon(LocalDate.now().plusDays(10), BigDecimal.ZERO)),
            Collections.emptyList(),
            Collections.emptyList(),
            null);

        // Act
        var dto = InntektsmeldingMapper.mapTilDto(request);

        // Assert
        assertThat(dto.getAktørId().getAktørId()).isEqualTo(request.aktorId().id());
        assertThat(dto.getArbeidsgiver().orgnr()).isEqualTo(request.arbeidsgiverIdent().ident());
        assertThat(dto.getMånedInntekt()).isEqualByComparingTo(request.inntekt());
        assertThat(dto.getStartdato()).isEqualTo(request.startdato());
        assertThat(dto.getYtelse()).isEqualTo(KodeverkMapper.mapYtelsetype(request.ytelse()));
        assertThat(dto.getMånedRefusjon()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(dto.getOpphørsdatoRefusjon()).isEqualTo(LocalDate.now().plusDays(9));
        assertThat(dto.getKontaktperson().navn()).isEqualTo(request.kontaktperson().navn());
        assertThat(dto.getKontaktperson().telefonnummer()).isEqualTo(request.kontaktperson().telefonnummer());
        assertThat(dto.getSøkteRefusjonsperioder()).isEmpty();
    }

    @Test
    void skal_teste_dto_mapping_med_ref_opphør_endring() {
        // Arrange
        var request = new SendInntektsmeldingRequestDto(UUID.randomUUID(),
            new AktørIdDto("9999999999999"),
            YtelseTypeDto.FORELDREPENGER,
            new ArbeidsgiverDto("999999999"),
            new SendInntektsmeldingRequestDto.KontaktpersonRequestDto("Testy test", "999999999"),
            LocalDate.now(),
            BigDecimal.valueOf(5000),
            Arrays.asList(new SendInntektsmeldingRequestDto.Refusjon(LocalDate.now(), BigDecimal.valueOf(5000)),
                new SendInntektsmeldingRequestDto.Refusjon(LocalDate.now().plusDays(5), BigDecimal.valueOf(4000)),
                new SendInntektsmeldingRequestDto.Refusjon(LocalDate.now().plusDays(10), BigDecimal.ZERO)),
            Collections.emptyList(),
            Collections.emptyList(),
            null);

        // Act
        var dto = InntektsmeldingMapper.mapTilDto(request);

        // Assert
        assertThat(dto.getAktørId().getAktørId()).isEqualTo(request.aktorId().id());
        assertThat(dto.getArbeidsgiver().orgnr()).isEqualTo(request.arbeidsgiverIdent().ident());
        assertThat(dto.getMånedInntekt()).isEqualByComparingTo(request.inntekt());
        assertThat(dto.getStartdato()).isEqualTo(request.startdato());
        assertThat(dto.getYtelse()).isEqualTo(KodeverkMapper.mapYtelsetype(request.ytelse()));
        assertThat(dto.getMånedRefusjon()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(dto.getOpphørsdatoRefusjon()).isEqualTo(LocalDate.now().plusDays(9));
        assertThat(dto.getKontaktperson().navn()).isEqualTo(request.kontaktperson().navn());
        assertThat(dto.getKontaktperson().telefonnummer()).isEqualTo(request.kontaktperson().telefonnummer());
        assertThat(dto.getSøkteRefusjonsperioder()).hasSize(1);
        assertThat(dto.getSøkteRefusjonsperioder().getFirst().fom()).isEqualTo(LocalDate.now().plusDays(5));
        assertThat(dto.getSøkteRefusjonsperioder().getFirst().beløp()).isEqualByComparingTo(BigDecimal.valueOf(4000));
    }

    @Test
    void skal_teste_dto_mapping_med_ref_og_naturalytelse_og_endringsårsak() {
        // Arrange
        var request = new SendInntektsmeldingRequestDto(UUID.randomUUID(),
            new AktørIdDto("9999999999999"),
            YtelseTypeDto.FORELDREPENGER,
            new ArbeidsgiverDto("999999999"),
            new SendInntektsmeldingRequestDto.KontaktpersonRequestDto("Testy test", "999999999"),
            LocalDate.now(),
            BigDecimal.valueOf(5000),
            Arrays.asList(new SendInntektsmeldingRequestDto.Refusjon(LocalDate.now(), BigDecimal.valueOf(5000)),
                new SendInntektsmeldingRequestDto.Refusjon(LocalDate.now().plusDays(10), BigDecimal.ZERO)),
            Collections.singletonList(
                new SendInntektsmeldingRequestDto.BortfaltNaturalytelseRequestDto(LocalDate.now(),
                    Tid.TIDENES_ENDE,
                    NaturalytelsetypeDto.ANNET,
                    BigDecimal.valueOf(4000))),
            Collections.singletonList(new SendInntektsmeldingRequestDto.EndringsårsakerRequestDto(EndringsårsakDto.TARIFFENDRING,
                null,
                null,
                LocalDate.now())), null);

        // Act
        var dto = InntektsmeldingMapper.mapTilDto(request);

        // Assert
        assertThat(dto.getAktørId().getAktørId()).isEqualTo(request.aktorId().id());
        assertThat(dto.getArbeidsgiver().orgnr()).isEqualTo(request.arbeidsgiverIdent().ident());
        assertThat(dto.getMånedInntekt()).isEqualByComparingTo(request.inntekt());
        assertThat(dto.getStartdato()).isEqualTo(request.startdato());
        assertThat(dto.getYtelse()).isEqualTo(KodeverkMapper.mapYtelsetype(request.ytelse()));
        assertThat(dto.getMånedRefusjon()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(dto.getOpphørsdatoRefusjon()).isEqualTo(LocalDate.now().plusDays(9));
        assertThat(dto.getKontaktperson().navn()).isEqualTo(request.kontaktperson().navn());
        assertThat(dto.getKontaktperson().telefonnummer()).isEqualTo(request.kontaktperson().telefonnummer());

        assertThat(dto.getEndringAvInntektÅrsaker()).hasSize(1);
        assertThat(dto.getEndringAvInntektÅrsaker().get(0).årsak()).isEqualTo(Endringsårsak.TARIFFENDRING);
        assertThat(dto.getEndringAvInntektÅrsaker().get(0).bleKjentFom()).isEqualTo(LocalDate.now());

        assertThat(dto.getBortfaltNaturalytelsePerioder()).hasSize(1);
        assertThat(dto.getBortfaltNaturalytelsePerioder().getFirst().beløp()).isEqualByComparingTo(
            request.bortfaltNaturalytelsePerioder().getFirst().beløp());
        assertThat(dto.getBortfaltNaturalytelsePerioder().getFirst().naturalytelsetype()).isEqualTo(
            KodeverkMapper.mapNaturalytelseTilDomene(request.bortfaltNaturalytelsePerioder().getFirst().naturalytelsetype()));
        assertThat(dto.getBortfaltNaturalytelsePerioder().getFirst().fom()).isEqualTo(request.bortfaltNaturalytelsePerioder()
            .getFirst()
            .fom());
        assertThat(dto.getBortfaltNaturalytelsePerioder().getFirst().tom()).isEqualTo(request.bortfaltNaturalytelsePerioder()
            .getFirst()
            .tom());
    }


    // ---- Tests for mapFraDomene (InntektsmeldingDto -> InntektsmeldingResponseDto) ----

    @Test
    void skal_teste_mapping_tilbake_til_dto_refusjon_og_opphør() {
        // Arrange
        var opprettetTidspunkt = LocalDateTime.now().plusDays(1);
        var imDto = InntektsmeldingDto.builder()
            .medAktørId(new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId("9999999999999"))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("999999999", "Første"))
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medInntekt(BigDecimal.valueOf(5000))
            .medMånedRefusjon(BigDecimal.valueOf(5000))
            .medOpphørsdatoRefusjon(LocalDate.now().plusDays(5))
            .medStartdato(LocalDate.now())
            .medArbeidsgiver(new Arbeidsgiver("999999999"))
            .medInnsendtTidspunkt(opprettetTidspunkt)
            .medBortfaltNaturalytelsePerioder(List.of(
                new InntektsmeldingDto.BortfaltNaturalytelse(LocalDate.now(), Tid.TIDENES_ENDE, NaturalytelseType.LOSJI, new BigDecimal(20)),
                new InntektsmeldingDto.BortfaltNaturalytelse(LocalDate.now(), LocalDate.now().plusMonths(1), NaturalytelseType.BIL, new BigDecimal(77))
            ))
            .medEndringAvInntektÅrsaker(List.of(
                new InntektsmeldingDto.Endringsårsaker(Endringsårsak.FERIE, LocalDate.now(), LocalDate.now().plusDays(10), null),
                new InntektsmeldingDto.Endringsårsaker(Endringsårsak.TARIFFENDRING, null, null, LocalDate.now())
            ))
            .medSøkteRefusjonsperioder(List.of())
            .build();

        var forespørselEntitet = new ForespørselEntitet("999999999",
            LocalDate.now(),
            new AktørIdEntitet("9999999999999"),
            Ytelsetype.FORELDREPENGER,
            "123",
            LocalDate.now(),
            ForespørselType.BESTILT_AV_FAGSYSTEM);

        // Act
        var result = InntektsmeldingMapper.mapFraDomene(imDto, ForespørselDtoMapper.mapFraEntitet(forespørselEntitet));

        // Assert
        assertThat(result.aktorId().id()).isEqualTo("9999999999999");
        assertThat(result.arbeidsgiverIdent().ident()).isEqualTo("999999999");
        assertThat(result.inntekt()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(result.startdato()).isEqualTo(LocalDate.now());
        assertThat(KodeverkMapper.mapYtelsetype(result.ytelse())).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(result.kontaktperson().navn()).isEqualTo("Første");
        assertThat(result.kontaktperson().telefonnummer()).isEqualTo("999999999");
        assertThat(result.bortfaltNaturalytelsePerioder()).hasSize(2);
        assertThat(result.bortfaltNaturalytelsePerioder().get(0).tom()).isNull();
        assertThat(result.bortfaltNaturalytelsePerioder().get(0).fom()).isEqualTo(LocalDate.now());
        assertThat(result.refusjon()).hasSize(2);
        assertThat(result.refusjon().get(0).fom()).isEqualTo(LocalDate.now());
        assertThat(result.refusjon().get(0).beløp()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(result.refusjon().get(1).fom()).isEqualTo(LocalDate.now().plusDays(6));
        assertThat(result.refusjon().get(1).beløp()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.opprettetTidspunkt()).isEqualTo(opprettetTidspunkt);
        assertThat(result.endringAvInntektÅrsaker()).hasSize(2);
        assertThat(result.endringAvInntektÅrsaker().get(0).årsak()).isEqualTo(EndringsårsakDto.FERIE);
        assertThat(result.endringAvInntektÅrsaker().get(1).årsak()).isEqualTo(EndringsårsakDto.TARIFFENDRING);
        assertThat(result.arbeidsgiverinitiertÅrsak()).isNull();
    }

    @Test
    void skal_teste_mapping_tilbake_til_dto_refusjon_opphør_endring() {
        // Arrange
        var opprettetTidspunkt = LocalDateTime.now().plusDays(1);
        var imDto = InntektsmeldingDto.builder()
            .medAktørId(new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId("9999999999999"))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("999999999", "Første"))
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medInntekt(BigDecimal.valueOf(5000))
            .medMånedRefusjon(BigDecimal.valueOf(5000))
            .medOpphørsdatoRefusjon(LocalDate.now().plusDays(10))
            .medStartdato(LocalDate.now())
            .medArbeidsgiver(new Arbeidsgiver("999999999"))
            .medInnsendtTidspunkt(opprettetTidspunkt)
            .medSøkteRefusjonsperioder(List.of(new InntektsmeldingDto.SøktRefusjon(LocalDate.now().plusDays(5), BigDecimal.valueOf(4000))))
            .medBortfaltNaturalytelsePerioder(List.of(
                new InntektsmeldingDto.BortfaltNaturalytelse(LocalDate.now(), Tid.TIDENES_ENDE, NaturalytelseType.LOSJI, new BigDecimal(20)),
                new InntektsmeldingDto.BortfaltNaturalytelse(LocalDate.now(), LocalDate.now().plusMonths(1), NaturalytelseType.BIL, new BigDecimal(77))
            ))
            .medEndringAvInntektÅrsaker(List.of(
                new InntektsmeldingDto.Endringsårsaker(Endringsårsak.FERIE, LocalDate.now(), LocalDate.now().plusDays(10), null),
                new InntektsmeldingDto.Endringsårsaker(Endringsårsak.TARIFFENDRING, null, null, LocalDate.now())
            ))
            .build();

        var forespørselEntitet = new ForespørselEntitet("999999999",
            LocalDate.now(),
            new AktørIdEntitet("9999999999999"),
            Ytelsetype.FORELDREPENGER,
            "123",
            LocalDate.now(),
            ForespørselType.BESTILT_AV_FAGSYSTEM);

        // Act
        var result = InntektsmeldingMapper.mapFraDomene(imDto, ForespørselDtoMapper.mapFraEntitet(forespørselEntitet));

        // Assert
        assertThat(result.aktorId().id()).isEqualTo("9999999999999");
        assertThat(result.arbeidsgiverIdent().ident()).isEqualTo("999999999");
        assertThat(result.inntekt()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(result.startdato()).isEqualTo(LocalDate.now());
        assertThat(KodeverkMapper.mapYtelsetype(result.ytelse())).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(result.kontaktperson().navn()).isEqualTo("Første");
        assertThat(result.kontaktperson().telefonnummer()).isEqualTo("999999999");
        assertThat(result.bortfaltNaturalytelsePerioder()).hasSize(2);
        assertThat(result.bortfaltNaturalytelsePerioder().get(0).tom()).isNull();
        assertThat(result.bortfaltNaturalytelsePerioder().get(0).fom()).isEqualTo(LocalDate.now());
        assertThat(result.refusjon()).hasSize(3);
        assertThat(result.refusjon().get(0).fom()).isEqualTo(LocalDate.now());
        assertThat(result.refusjon().get(0).beløp()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(result.refusjon().get(1).fom()).isEqualTo(LocalDate.now().plusDays(5));
        assertThat(result.refusjon().get(1).beløp()).isEqualByComparingTo(BigDecimal.valueOf(4000));
        assertThat(result.refusjon().get(2).fom()).isEqualTo(LocalDate.now().plusDays(11));
        assertThat(result.refusjon().get(2).beløp()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.opprettetTidspunkt()).isEqualTo(opprettetTidspunkt);
        assertThat(result.endringAvInntektÅrsaker()).hasSize(2);
        assertThat(result.endringAvInntektÅrsaker().get(0).årsak()).isEqualTo(EndringsårsakDto.FERIE);
        assertThat(result.endringAvInntektÅrsaker().get(1).årsak()).isEqualTo(EndringsårsakDto.TARIFFENDRING);
        assertThat(result.arbeidsgiverinitiertÅrsak()).isNull();
    }

    @Test
    void skal_teste_mapping_tilbake_til_dto_refusjon_ikke_opphør_eller_endring() {
        // Arrange
        var opprettetTidspunkt = LocalDateTime.now().plusDays(1);
        var imDto = InntektsmeldingDto.builder()
            .medAktørId(new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId("9999999999999"))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("999999999", "Første"))
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medInntekt(BigDecimal.valueOf(5000))
            .medMånedRefusjon(BigDecimal.valueOf(5000))
            .medOpphørsdatoRefusjon(Tid.TIDENES_ENDE)
            .medStartdato(LocalDate.now())
            .medArbeidsgiver(new Arbeidsgiver("999999999"))
            .medInnsendtTidspunkt(opprettetTidspunkt)
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(List.of(
                new InntektsmeldingDto.BortfaltNaturalytelse(LocalDate.now(), Tid.TIDENES_ENDE, NaturalytelseType.LOSJI, new BigDecimal(20)),
                new InntektsmeldingDto.BortfaltNaturalytelse(LocalDate.now(), LocalDate.now().plusMonths(1), NaturalytelseType.BIL, new BigDecimal(77))
            ))
            .medEndringAvInntektÅrsaker(List.of(
                new InntektsmeldingDto.Endringsårsaker(Endringsårsak.FERIE, LocalDate.now(), LocalDate.now().plusDays(10), null),
                new InntektsmeldingDto.Endringsårsaker(Endringsårsak.TARIFFENDRING, null, null, LocalDate.now())
            ))
            .build();

        var forespørselEntitet = new ForespørselEntitet("999999999",
            LocalDate.now(),
            new AktørIdEntitet("9999999999999"),
            Ytelsetype.FORELDREPENGER,
            "123",
            LocalDate.now(),
            ForespørselType.BESTILT_AV_FAGSYSTEM);

        // Act
        var result = InntektsmeldingMapper.mapFraDomene(imDto, ForespørselDtoMapper.mapFraEntitet(forespørselEntitet));

        // Assert
        assertThat(result.aktorId().id()).isEqualTo("9999999999999");
        assertThat(result.arbeidsgiverIdent().ident()).isEqualTo("999999999");
        assertThat(result.inntekt()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(result.startdato()).isEqualTo(LocalDate.now());
        assertThat(KodeverkMapper.mapYtelsetype(result.ytelse())).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(result.kontaktperson().navn()).isEqualTo("Første");
        assertThat(result.kontaktperson().telefonnummer()).isEqualTo("999999999");
        assertThat(result.bortfaltNaturalytelsePerioder()).hasSize(2);
        assertThat(result.bortfaltNaturalytelsePerioder().get(0).tom()).isNull();
        assertThat(result.bortfaltNaturalytelsePerioder().get(0).fom()).isEqualTo(LocalDate.now());
        assertThat(result.refusjon()).hasSize(1);
        assertThat(result.refusjon().get(0).fom()).isEqualTo(LocalDate.now());
        assertThat(result.refusjon().get(0).beløp()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(result.opprettetTidspunkt()).isEqualTo(opprettetTidspunkt);
        assertThat(result.endringAvInntektÅrsaker()).hasSize(2);
        assertThat(result.endringAvInntektÅrsaker().get(0).årsak()).isEqualTo(EndringsårsakDto.FERIE);
        assertThat(result.endringAvInntektÅrsaker().get(1).årsak()).isEqualTo(EndringsårsakDto.TARIFFENDRING);
        assertThat(result.arbeidsgiverinitiertÅrsak()).isNull();
    }

    @Test
    void skal_teste_mapping_av_arbeidsgiverinitiert_inntektsmelding() {
        // Arrange
        var opprettetTidspunkt = LocalDateTime.now().plusDays(1);
        var imDto = InntektsmeldingDto.builder()
            .medAktørId(new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId("9999999999999"))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("999999999", "Første"))
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medInntekt(BigDecimal.valueOf(5000))
            .medMånedRefusjon(BigDecimal.valueOf(5000))
            .medOpphørsdatoRefusjon(Tid.TIDENES_ENDE)
            .medStartdato(LocalDate.now())
            .medArbeidsgiver(new Arbeidsgiver("999999999"))
            .medInnsendtTidspunkt(opprettetTidspunkt)
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(List.of())
            .medEndringAvInntektÅrsaker(List.of())
            .build();

        var forespørselEntitet = new ForespørselEntitet("999999999",
            LocalDate.now(),
            new AktørIdEntitet("9999999999999"),
            Ytelsetype.FORELDREPENGER,
            "123",
            LocalDate.now(),
            ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);

        // Act
        var result = InntektsmeldingMapper.mapFraDomene(imDto, ForespørselDtoMapper.mapFraEntitet(forespørselEntitet));

        // Assert
        assertThat(result.aktorId().id()).isEqualTo("9999999999999");
        assertThat(result.arbeidsgiverIdent().ident()).isEqualTo("999999999");
        assertThat(result.inntekt()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(result.startdato()).isEqualTo(LocalDate.now());
        assertThat(KodeverkMapper.mapYtelsetype(result.ytelse())).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(result.kontaktperson().navn()).isEqualTo("Første");
        assertThat(result.kontaktperson().telefonnummer()).isEqualTo("999999999");
        assertThat(result.refusjon()).hasSize(1);
        assertThat(result.refusjon().get(0).fom()).isEqualTo(LocalDate.now());
        assertThat(result.refusjon().get(0).beløp()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(result.opprettetTidspunkt()).isEqualTo(opprettetTidspunkt);
        assertThat(result.arbeidsgiverinitiertÅrsak()).isEqualTo(ArbeidsgiverinitiertÅrsakDto.NYANSATT);
    }
}
