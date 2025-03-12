package no.nav.familie.inntektsmelding.imdialog.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.koder.Endringsårsak;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.EndringsårsakDto;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.NaturalytelsetypeDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;

class InntektsmeldingMapperTest {

    @Test
    void skal_teste_mapping_uten_ref_og_naturalytelse() {
        // Arrange
        var request = new SendInntektsmeldingRequestDto(UUID.randomUUID(), new AktørIdDto("9999999999999"), YtelseTypeDto.FORELDREPENGER,
            new ArbeidsgiverDto("999999999"), new SendInntektsmeldingRequestDto.KontaktpersonRequestDto("Testy test", "999999999"), LocalDate.now(),
            BigDecimal.valueOf(5000), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null);

        // Act
        var entitet = InntektsmeldingMapper.mapTilEntitet(request);

        // Assert
        assertThat(entitet.getAktørId().getAktørId()).isEqualTo(request.aktorId().id());
        assertThat(entitet.getArbeidsgiverIdent()).isEqualTo(request.arbeidsgiverIdent().ident());
        assertThat(entitet.getMånedInntekt()).isEqualByComparingTo(request.inntekt());
        assertThat(entitet.getStartDato()).isEqualTo(request.startdato());
        assertThat(entitet.getYtelsetype()).isEqualTo(KodeverkMapper.mapYtelsetype(request.ytelse()));
        assertThat(entitet.getKontaktperson().getNavn()).isEqualTo(request.kontaktperson().navn());
        assertThat(entitet.getKontaktperson().getTelefonnummer()).isEqualTo(request.kontaktperson().telefonnummer());
        assertThat(entitet.getBorfalteNaturalYtelser()).isEmpty();
        assertThat(entitet.getMånedRefusjon()).isNull();
        assertThat(entitet.getOpphørsdatoRefusjon()).isNull();
    }

    @Test
    void skal_teste_mapping_med_ref_opphør() {
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
        var entitet = InntektsmeldingMapper.mapTilEntitet(request);

        // Assert
        assertThat(entitet.getAktørId().getAktørId()).isEqualTo(request.aktorId().id());
        assertThat(entitet.getArbeidsgiverIdent()).isEqualTo(request.arbeidsgiverIdent().ident());
        assertThat(entitet.getMånedInntekt()).isEqualByComparingTo(request.inntekt());
        assertThat(entitet.getStartDato()).isEqualTo(request.startdato());
        assertThat(entitet.getYtelsetype()).isEqualTo(KodeverkMapper.mapYtelsetype(request.ytelse()));
        assertThat(entitet.getMånedRefusjon()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(entitet.getOpphørsdatoRefusjon()).isEqualTo(LocalDate.now().plusDays(9));
        assertThat(entitet.getKontaktperson().getNavn()).isEqualTo(request.kontaktperson().navn());
        assertThat(entitet.getKontaktperson().getTelefonnummer()).isEqualTo(request.kontaktperson().telefonnummer());
        assertThat(entitet.getRefusjonsendringer()).isEmpty();
    }

    @Test
    void skal_teste_mapping_med_ref_opphør_endring() {
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
        var entitet = InntektsmeldingMapper.mapTilEntitet(request);

        // Assert
        assertThat(entitet.getAktørId().getAktørId()).isEqualTo(request.aktorId().id());
        assertThat(entitet.getArbeidsgiverIdent()).isEqualTo(request.arbeidsgiverIdent().ident());
        assertThat(entitet.getMånedInntekt()).isEqualByComparingTo(request.inntekt());
        assertThat(entitet.getStartDato()).isEqualTo(request.startdato());
        assertThat(entitet.getYtelsetype()).isEqualTo(KodeverkMapper.mapYtelsetype(request.ytelse()));
        assertThat(entitet.getMånedRefusjon()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(entitet.getOpphørsdatoRefusjon()).isEqualTo(LocalDate.now().plusDays(9));
        assertThat(entitet.getKontaktperson().getNavn()).isEqualTo(request.kontaktperson().navn());
        assertThat(entitet.getKontaktperson().getTelefonnummer()).isEqualTo(request.kontaktperson().telefonnummer());
        assertThat(entitet.getRefusjonsendringer()).hasSize(1);
        assertThat(entitet.getRefusjonsendringer().getFirst().getFom()).isEqualTo(LocalDate.now().plusDays(5));
        assertThat(entitet.getRefusjonsendringer().getFirst().getRefusjonPrMnd()).isEqualByComparingTo(BigDecimal.valueOf(4000));
    }

    @Test
    void skal_teste_mapping_med_ref_og_naturalytelse_og_endringsårsak() {
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
        var entitet = InntektsmeldingMapper.mapTilEntitet(request);

        // Assert
        assertThat(entitet.getAktørId().getAktørId()).isEqualTo(request.aktorId().id());
        assertThat(entitet.getArbeidsgiverIdent()).isEqualTo(request.arbeidsgiverIdent().ident());
        assertThat(entitet.getMånedInntekt()).isEqualByComparingTo(request.inntekt());
        assertThat(entitet.getStartDato()).isEqualTo(request.startdato());
        assertThat(entitet.getYtelsetype()).isEqualTo(KodeverkMapper.mapYtelsetype(request.ytelse()));
        assertThat(entitet.getMånedRefusjon()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(entitet.getOpphørsdatoRefusjon()).isEqualTo(LocalDate.now().plusDays(9));
        assertThat(entitet.getKontaktperson().getNavn()).isEqualTo(request.kontaktperson().navn());
        assertThat(entitet.getKontaktperson().getTelefonnummer()).isEqualTo(request.kontaktperson().telefonnummer());

        assertThat(entitet.getEndringsårsaker()).hasSize(1);
        assertThat(entitet.getEndringsårsaker().get(0).getÅrsak()).isEqualTo(Endringsårsak.TARIFFENDRING);
        assertThat(entitet.getEndringsårsaker().get(0).getBleKjentFom().orElse(null)).isEqualTo(LocalDate.now());

        assertThat(entitet.getBorfalteNaturalYtelser()).hasSize(1);
        assertThat(entitet.getBorfalteNaturalYtelser().getFirst().getMånedBeløp()).isEqualByComparingTo(
            request.bortfaltNaturalytelsePerioder().getFirst().beløp());
        assertThat(entitet.getBorfalteNaturalYtelser().getFirst().getType()).isEqualByComparingTo(
            KodeverkMapper.mapNaturalytelseTilEntitet(request.bortfaltNaturalytelsePerioder().getFirst().naturalytelsetype()));
        assertThat(entitet.getBorfalteNaturalYtelser().getFirst().getPeriode().getFom()).isEqualTo(request.bortfaltNaturalytelsePerioder()
            .getFirst()
            .fom());
        assertThat(entitet.getBorfalteNaturalYtelser().getFirst().getPeriode().getTom()).isEqualTo(request.bortfaltNaturalytelsePerioder()
            .getFirst()
            .tom());
    }

    @Test
    void skal_teste_mapping_tilbake_til_dto_refusjon_og_opphør() {
        // Arrange
        var imEntitet = InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet("9999999999999"))
            .medKontaktperson(new KontaktpersonEntitet("Første", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(5000))
            .medMånedRefusjon(BigDecimal.valueOf(5000))
            .medRefusjonOpphørsdato(LocalDate.now().plusDays(5))
            .medStartDato(LocalDate.now())
            .medArbeidsgiverIdent("999999999")
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(1))
            .medBortfaltNaturalytelser(List.of(
                    BortaltNaturalytelseEntitet.builder()
                        .medPeriode(LocalDate.now(), Tid.TIDENES_ENDE)
                        .medType(NaturalytelseType.LOSJI)
                        .medMånedBeløp(new BigDecimal(20))
                        .build(),
                    BortaltNaturalytelseEntitet.builder()
                        .medPeriode(LocalDate.now(), LocalDate.now().plusMonths(1))
                        .medType(NaturalytelseType.BIL)
                        .medMånedBeløp(new BigDecimal(77))
                        .build()
                )
            )
            .medEndringsårsaker(List.of(
                EndringsårsakEntitet.builder()
                    .medFom(LocalDate.now())
                    .medTom(LocalDate.now().plusDays(10))
                    .medÅrsak(Endringsårsak.FERIE)
                    .build(),
                EndringsårsakEntitet.builder()
                    .medBleKjentFra(LocalDate.now())
                    .medÅrsak(Endringsårsak.TARIFFENDRING)
                    .build()
            ))
            .build();

        var forespørselUuid = UUID.randomUUID();

        // Act
        var imDto = InntektsmeldingMapper.mapFraEntitet(imEntitet, forespørselUuid);

        // Assert
        assertThat(imDto.aktorId().id()).isEqualTo(imEntitet.getAktørId().getAktørId());
        assertThat(imDto.arbeidsgiverIdent().ident()).isEqualTo(imEntitet.getArbeidsgiverIdent());
        assertThat(imDto.inntekt()).isEqualByComparingTo(imEntitet.getMånedInntekt());
        assertThat(imDto.startdato()).isEqualTo(imEntitet.getStartDato());
        assertThat(KodeverkMapper.mapYtelsetype(imDto.ytelse())).isEqualTo(imEntitet.getYtelsetype());
        assertThat(BigDecimal.valueOf(5000)).isEqualByComparingTo(imEntitet.getMånedRefusjon());
        assertThat(imDto.kontaktperson().navn()).isEqualTo(imEntitet.getKontaktperson().getNavn());
        assertThat(imDto.kontaktperson().telefonnummer()).isEqualTo(imEntitet.getKontaktperson().getTelefonnummer());
        assertThat(imDto.bortfaltNaturalytelsePerioder()).hasSize(2);
        assertThat(imDto.bortfaltNaturalytelsePerioder().get(0).tom()).isNull();
        assertThat(imDto.bortfaltNaturalytelsePerioder().get(0).fom()).isEqualTo(imEntitet.getBorfalteNaturalYtelser().get(0).getPeriode().getFom());
        assertThat(imDto.refusjon()).hasSize(2);
        assertThat(imDto.refusjon().get(0).fom()).isEqualTo(imEntitet.getStartDato());
        assertThat(imDto.refusjon().get(0).beløp()).isEqualByComparingTo(imEntitet.getMånedRefusjon());
        assertThat(imDto.refusjon().get(1).fom()).isEqualTo(LocalDate.now().plusDays(6));
        assertThat(imDto.refusjon().get(1).beløp()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(imDto.opprettetTidspunkt()).isEqualTo(imEntitet.getOpprettetTidspunkt());
        assertThat(imDto.endringAvInntektÅrsaker()).hasSize(2);
        assertThat(imDto.endringAvInntektÅrsaker().get(0).årsak()).isEqualTo(EndringsårsakDto.FERIE);
        assertThat(imDto.endringAvInntektÅrsaker().get(1).årsak()).isEqualTo(EndringsårsakDto.TARIFFENDRING);
    }

    @Test
    void skal_teste_mapping_tilbake_til_dto_refusjon_opphør_endring() {
        // Arrange
        var imEntitet = InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet("9999999999999"))
            .medKontaktperson(new KontaktpersonEntitet("Første", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(5000))
            .medMånedRefusjon(BigDecimal.valueOf(5000))
            .medRefusjonOpphørsdato(LocalDate.now().plusDays(10))
            .medStartDato(LocalDate.now())
            .medArbeidsgiverIdent("999999999")
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(1))
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(LocalDate.now().plusDays(5), BigDecimal.valueOf(4000))))
            .medBortfaltNaturalytelser(List.of(
                    BortaltNaturalytelseEntitet.builder()
                        .medPeriode(LocalDate.now(), Tid.TIDENES_ENDE)
                        .medType(NaturalytelseType.LOSJI)
                        .medMånedBeløp(new BigDecimal(20))
                        .build(),
                    BortaltNaturalytelseEntitet.builder()
                        .medPeriode(LocalDate.now(), LocalDate.now().plusMonths(1))
                        .medType(NaturalytelseType.BIL)
                        .medMånedBeløp(new BigDecimal(77))
                        .build()
                )
            )
            .medEndringsårsaker(List.of(
                EndringsårsakEntitet.builder()
                    .medFom(LocalDate.now())
                    .medTom(LocalDate.now().plusDays(10))
                    .medÅrsak(Endringsårsak.FERIE)
                    .build(),
                EndringsårsakEntitet.builder()
                    .medBleKjentFra(LocalDate.now())
                    .medÅrsak(Endringsårsak.TARIFFENDRING)
                    .build()
            ))
            .build();

        var forespørselUuid = UUID.randomUUID();

        // Act
        var imDto = InntektsmeldingMapper.mapFraEntitet(imEntitet, forespørselUuid);

        // Assert
        assertThat(imDto.aktorId().id()).isEqualTo(imEntitet.getAktørId().getAktørId());
        assertThat(imDto.arbeidsgiverIdent().ident()).isEqualTo(imEntitet.getArbeidsgiverIdent());
        assertThat(imDto.inntekt()).isEqualByComparingTo(imEntitet.getMånedInntekt());
        assertThat(imDto.startdato()).isEqualTo(imEntitet.getStartDato());
        assertThat(KodeverkMapper.mapYtelsetype(imDto.ytelse())).isEqualTo(imEntitet.getYtelsetype());
        assertThat(BigDecimal.valueOf(5000)).isEqualByComparingTo(imEntitet.getMånedRefusjon());
        assertThat(imDto.kontaktperson().navn()).isEqualTo(imEntitet.getKontaktperson().getNavn());
        assertThat(imDto.kontaktperson().telefonnummer()).isEqualTo(imEntitet.getKontaktperson().getTelefonnummer());
        assertThat(imDto.bortfaltNaturalytelsePerioder()).hasSize(2);
        assertThat(imDto.bortfaltNaturalytelsePerioder().get(0).tom()).isNull();
        assertThat(imDto.bortfaltNaturalytelsePerioder().get(0).fom()).isEqualTo(imEntitet.getBorfalteNaturalYtelser().get(0).getPeriode().getFom());
        assertThat(imDto.refusjon()).hasSize(3);
        assertThat(imDto.refusjon().get(0).fom()).isEqualTo(imEntitet.getStartDato());
        assertThat(imDto.refusjon().get(0).beløp()).isEqualByComparingTo(imEntitet.getMånedRefusjon());
        assertThat(imDto.refusjon().get(1).fom()).isEqualTo(LocalDate.now().plusDays(5));
        assertThat(imDto.refusjon().get(1).beløp()).isEqualByComparingTo(BigDecimal.valueOf(4000));
        assertThat(imDto.refusjon().get(2).fom()).isEqualTo(LocalDate.now().plusDays(11));
        assertThat(imDto.refusjon().get(2).beløp()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(imDto.opprettetTidspunkt()).isEqualTo(imEntitet.getOpprettetTidspunkt());
        assertThat(imDto.endringAvInntektÅrsaker()).hasSize(2);
        assertThat(imDto.endringAvInntektÅrsaker().get(0).årsak()).isEqualTo(EndringsårsakDto.FERIE);
        assertThat(imDto.endringAvInntektÅrsaker().get(1).årsak()).isEqualTo(EndringsårsakDto.TARIFFENDRING);

    }

    @Test
    void skal_teste_mapping_tilbake_til_dto_refusjon_ikke_opphør_eller_endring() {
        // Arrange
        var imEntitet = InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet("9999999999999"))
            .medKontaktperson(new KontaktpersonEntitet("Første", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(5000))
            .medMånedRefusjon(BigDecimal.valueOf(5000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(LocalDate.now())
            .medArbeidsgiverIdent("999999999")
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(1))
            .medBortfaltNaturalytelser(List.of(
                    BortaltNaturalytelseEntitet.builder()
                        .medPeriode(LocalDate.now(), Tid.TIDENES_ENDE)
                        .medType(NaturalytelseType.LOSJI)
                        .medMånedBeløp(new BigDecimal(20))
                        .build(),
                    BortaltNaturalytelseEntitet.builder()
                        .medPeriode(LocalDate.now(), LocalDate.now().plusMonths(1))
                        .medType(NaturalytelseType.BIL)
                        .medMånedBeløp(new BigDecimal(77))
                        .build()
                )
            )
            .medEndringsårsaker(List.of(
                EndringsårsakEntitet.builder()
                    .medFom(LocalDate.now())
                    .medTom(LocalDate.now().plusDays(10))
                    .medÅrsak(Endringsårsak.FERIE)
                    .build(),
                EndringsårsakEntitet.builder()
                    .medBleKjentFra(LocalDate.now())
                    .medÅrsak(Endringsårsak.TARIFFENDRING)
                    .build()
            ))
            .build();

        var forespørselUuid = UUID.randomUUID();

        // Act
        var imDto = InntektsmeldingMapper.mapFraEntitet(imEntitet, forespørselUuid);

        // Assert
        assertThat(imDto.aktorId().id()).isEqualTo(imEntitet.getAktørId().getAktørId());
        assertThat(imDto.arbeidsgiverIdent().ident()).isEqualTo(imEntitet.getArbeidsgiverIdent());
        assertThat(imDto.inntekt()).isEqualByComparingTo(imEntitet.getMånedInntekt());
        assertThat(imDto.startdato()).isEqualTo(imEntitet.getStartDato());
        assertThat(KodeverkMapper.mapYtelsetype(imDto.ytelse())).isEqualTo(imEntitet.getYtelsetype());
        assertThat(BigDecimal.valueOf(5000)).isEqualByComparingTo(imEntitet.getMånedRefusjon());
        assertThat(imDto.kontaktperson().navn()).isEqualTo(imEntitet.getKontaktperson().getNavn());
        assertThat(imDto.kontaktperson().telefonnummer()).isEqualTo(imEntitet.getKontaktperson().getTelefonnummer());
        assertThat(imDto.bortfaltNaturalytelsePerioder()).hasSize(2);
        assertThat(imDto.bortfaltNaturalytelsePerioder().get(0).tom()).isNull();
        assertThat(imDto.bortfaltNaturalytelsePerioder().get(0).fom()).isEqualTo(imEntitet.getBorfalteNaturalYtelser().get(0).getPeriode().getFom());
        assertThat(imDto.refusjon()).hasSize(1);
        assertThat(imDto.refusjon().get(0).fom()).isEqualTo(imEntitet.getStartDato());
        assertThat(imDto.refusjon().get(0).beløp()).isEqualByComparingTo(imEntitet.getMånedRefusjon());
        assertThat(imDto.opprettetTidspunkt()).isEqualTo(imEntitet.getOpprettetTidspunkt());
        assertThat(imDto.endringAvInntektÅrsaker()).hasSize(2);
        assertThat(imDto.endringAvInntektÅrsaker().get(0).årsak()).isEqualTo(EndringsårsakDto.FERIE);
        assertThat(imDto.endringAvInntektÅrsaker().get(1).årsak()).isEqualTo(EndringsårsakDto.TARIFFENDRING);

    }
}
