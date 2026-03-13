package no.nav.familie.inntektsmelding.overstyring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.entitet.AktørId;
import no.nav.vedtak.konfig.Tid;

class InntektsmeldingOverstyringMapper {

    private InntektsmeldingOverstyringMapper() {
    }

    public static InntektsmeldingEntitet mapTilEntitet(SendOverstyrtInntektsmeldingRequestDto dto) {
        return InntektsmeldingEntitet.builder()
            .medAktørId(new AktørId(dto.aktorId().id()))
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
                .medType(KodeverkMapper.mapNaturalytelseTilEntitet(d.naturalytelsetype()))
                .build())
            .toList();
    }
}
