package no.nav.foreldrepenger.inntektsmelding.overstyring.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.BortaltNaturalytelseEntitet;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.InntektsmeldingEntitet;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.RefusjonsendringEntitet;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;

class InntektsmeldingOverstyringMapper {

    private InntektsmeldingOverstyringMapper() {
    }

    // ---- Mapping til InntektsmeldingDto (domenemodell) ----

    public static InntektsmeldingDto mapTilDto(SendOverstyrtInntektsmeldingRequestDto dto) {
        var opphørsdato = finnOpphørsdato(dto.refusjonsendringer()).orElse(Tid.TIDENES_ENDE);
        return InntektsmeldingDto.builder()
            .medAktørId(AktørId.fra(dto.aktorId().id()))
            .medArbeidsgiver(Arbeidsgiver.fra(dto.arbeidsgiverIdent().ident()))
            .medKildesystem(Kildesystem.FPSAK)
            .medOpprettetAv(dto.opprettetAv())
            .medInntekt(dto.inntekt())
            .medMånedRefusjon(dto.refusjon())
            .medOpphørsdatoRefusjon(opphørsdato)
            .medStartdato(dto.startdato())
            .medYtelse(KodeverkMapper.mapYtelsetype(dto.ytelse()))
            .medBortfaltNaturalytelsePerioder(mapBortfalteNaturalYtelserTilDto(dto.bortfaltNaturalytelsePerioder()))
            .medSøkteRefusjonsperioder(mapRefusjonsendringerTilDto(dto.refusjonsendringer()))
            .medEndringAvInntektÅrsaker(List.of())
            .build();
    }

    private static List<InntektsmeldingDto.BortfaltNaturalytelse> mapBortfalteNaturalYtelserTilDto(
        List<SendOverstyrtInntektsmeldingRequestDto.BortfaltNaturalytelseRequestDto> dto) {
        return dto.stream()
            .map(d -> new InntektsmeldingDto.BortfaltNaturalytelse(
                d.fom(),
                d.tom() != null ? d.tom() : Tid.TIDENES_ENDE,
                KodeverkMapper.mapNaturalytelseTilDomene(d.naturalytelsetype()),
                d.beløp()))
            .toList();
    }

    private static List<InntektsmeldingDto.SøktRefusjon> mapRefusjonsendringerTilDto(
        List<SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto> refusjonsendringRequestDtos) {
        var sisteEndringSomOpphørerRef = finnSisteRefusjonsendring(refusjonsendringRequestDtos).filter(siste ->
            siste.beløp().compareTo(BigDecimal.ZERO) == 0);
        // Hvis siste periode med endring har refusjon == 0 trenger den ikke mappes som endring, legges på refusjonOpphører feltet
        return sisteEndringSomOpphørerRef.map(refusjonendringRequestDto -> refusjonsendringRequestDtos.stream()
                .filter(dto -> dto.fom().isBefore(refusjonendringRequestDto.fom()))
                .map(dto -> new InntektsmeldingDto.SøktRefusjon(dto.fom(), dto.beløp()))
                .toList())
            .orElseGet(() -> refusjonsendringRequestDtos.stream().map(dto -> new InntektsmeldingDto.SøktRefusjon(dto.fom(), dto.beløp())).toList());
    }

    // ---- Mapping til InntektsmeldingEntitet (database) - beholdt for bakoverkompatibilitet ----

    public static InntektsmeldingEntitet mapTilEntitet(SendOverstyrtInntektsmeldingRequestDto dto) {
        return InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet(dto.aktorId().id()))
            .medArbeidsgiverIdent(dto.arbeidsgiverIdent().orgnr())
            .medKildesystem(Kildesystem.FPSAK)
            .medOpprettetAv(dto.opprettetAv())
            .medMånedInntekt(dto.inntekt())
            .medMånedRefusjon(dto.refusjon())
            .medRefusjonOpphørsdato(finnOpphørsdato(dto.refusjonsendringer()).orElse(Tid.TIDENES_ENDE))
            .medStartDato(dto.startdato())
            .medYtelsetype(KodeverkMapper.mapYtelsetype(dto.ytelse()))
            .medBortfaltNaturalytelser(mapBortfalteNaturalytelser(dto.bortfaltNaturalytelsePerioder()))
            .medRefusjonsendringer(mapRefusjonsendringer(dto.refusjonsendringer()))
            .build();
    }

    private static Optional<LocalDate> finnOpphørsdato(
        List<SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto> refusjonsendringRequestDtos) {
        var sisteEndring = finnSisteRefusjonsendring(refusjonsendringRequestDtos);
        // Hvis siste endring setter refusjon til 0 er det å regne som opphør av refusjon,
        // setter dagen før denne endringen som opphørsdato
        return sisteEndring.filter(en -> en.beløp().compareTo(BigDecimal.ZERO) == 0)
            .map(sr -> sr.fom().minusDays(1));
    }

    private static List<RefusjonsendringEntitet> mapRefusjonsendringer(
        List<SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto> refusjonsendringRequestDtos) {
        var sisteEndringSomOpphørerRef = finnSisteRefusjonsendring(refusjonsendringRequestDtos).filter(siste ->
            siste.beløp().compareTo(BigDecimal.ZERO) == 0);
        // Hvis siste periode med endring har refusjon == 0 trenger den ikke mappes som endring, legges på refusjonOpphører feltet
        return sisteEndringSomOpphørerRef.map(refusjonendringRequestDto -> refusjonsendringRequestDtos.stream()
                .filter(dto -> dto.fom().isBefore(refusjonendringRequestDto.fom()))
                .map(dto -> new RefusjonsendringEntitet(dto.fom(), dto.beløp()))
                .toList())
            .orElseGet(() -> refusjonsendringRequestDtos.stream().map(dto -> new RefusjonsendringEntitet(dto.fom(), dto.beløp())).toList());
    }

    private static Optional<SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto> finnSisteRefusjonsendring(List<SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto> refusjonsendringRequestDtos) {
        return refusjonsendringRequestDtos.stream().max(Comparator.comparing(SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto::fom));
    }

    private static List<BortaltNaturalytelseEntitet> mapBortfalteNaturalytelser(
        List<SendOverstyrtInntektsmeldingRequestDto.BortfaltNaturalytelseRequestDto> dto) {
        return dto.stream()
            .map(d -> new BortaltNaturalytelseEntitet.Builder().medPeriode(d.fom(), d.tom() != null ? d.tom() : Tid.TIDENES_ENDE)
                .medMånedBeløp(d.beløp())
                .medType(KodeverkMapper.mapNaturalytelseTilDomene(d.naturalytelsetype()))
                .build())
            .toList();
    }
}
