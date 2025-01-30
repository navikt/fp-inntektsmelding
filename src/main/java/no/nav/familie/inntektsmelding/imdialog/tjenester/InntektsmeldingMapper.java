package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.NaturalytelsetypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;

public class InntektsmeldingMapper {

    private InntektsmeldingMapper() {
        // Skjuler default konstruktør
    }

    public static InntektsmeldingEntitet mapTilEntitet(SendInntektsmeldingRequestDto dto) {
        // Frontend sender kun inn liste med refusjon. Vi utleder startsum og opphørsdato utifra denne lista.
        var refusjonPrMnd = finnFørsteRefusjon(dto.refusjon(), dto.startdato()).orElse(null);
        var opphørsdato = refusjonPrMnd == null ? null : finnOpphørsdato(dto.refusjon(), dto.startdato()).orElse(Tid.TIDENES_ENDE);
        return InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet(dto.aktorId().id()))
            .medArbeidsgiverIdent(dto.arbeidsgiverIdent().ident())
            .medMånedInntekt(dto.inntekt())
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medMånedRefusjon(refusjonPrMnd)
            .medRefusjonOpphørsdato(opphørsdato)
            .medStartDato(dto.startdato())
            .medYtelsetype(KodeverkMapper.mapYtelsetype(dto.ytelse()))
            .medKontaktperson(mapKontaktPerson(dto))
            .medEndringsårsaker(mapEndringsårsaker(dto.endringAvInntektÅrsaker()))
            .medBortfaltNaturalytelser(mapBortfalteNaturalytelser(dto.bortfaltNaturalytelsePerioder()))
            .medRefusjonsendringer(mapRefusjonsendringer(dto.startdato(), opphørsdato, dto.refusjon()))
            .build();
    }

    private static Optional<BigDecimal> finnFørsteRefusjon(List<SendInntektsmeldingRequestDto.Refusjon> refusjon, LocalDate startdato) {
        if (refusjon.isEmpty()) {
            return Optional.empty();
        }
        var refusjonPåStartdato = refusjon.stream().filter(r -> r.fom().equals(startdato)).toList();
        if (refusjonPåStartdato.size() != 1) {
            throw new IllegalStateException("Forventer kun 1 refusjon som starter på startdato, fant " + refusjonPåStartdato.size());
        }
        return Optional.of(refusjonPåStartdato.getFirst().beløp());
    }

    private static List<EndringsårsakEntitet> mapEndringsårsaker(List<SendInntektsmeldingRequestDto.EndringsårsakerRequestDto> endringsårsaker) {
        return endringsårsaker.stream().map(InntektsmeldingMapper::mapEndringsårsak).toList();
    }

    private static EndringsårsakEntitet mapEndringsårsak(SendInntektsmeldingRequestDto.EndringsårsakerRequestDto e) {
        return EndringsårsakEntitet.builder()
            .medÅrsak(KodeverkMapper.mapEndringsårsak(e.årsak()))
            .medFom(e.fom())
            .medTom(e.tom())
            .medBleKjentFra(e.bleKjentFom())
            .build();
    }

    public static InntektsmeldingResponseDto mapFraEntitet(InntektsmeldingEntitet entitet, UUID forespørselUuid) {
        var refusjoner = mapRefusjonerTilDto(entitet);

        var bortfalteNaturalytelser = entitet.getBorfalteNaturalYtelser().stream().map(i ->
            new SendInntektsmeldingRequestDto.BortfaltNaturalytelseRequestDto(
                i.getPeriode().getFom(),
                Objects.equals(i.getPeriode().getTom(), Tid.TIDENES_ENDE) ? null : i.getPeriode().getTom(),
                NaturalytelsetypeDto.valueOf(i.getType().toString()),
                i.getMånedBeløp()
            )
        ).toList();
        var endringsårsaker = entitet.getEndringsårsaker().stream().map(e ->
            new SendInntektsmeldingRequestDto.EndringsårsakerRequestDto(KodeverkMapper.mapEndringsårsak(e.getÅrsak()),
                e.getFom().orElse(null),
                e.getTom().orElse(null),
                e.getBleKjentFom().orElse(null)))
            .toList();

        return new InntektsmeldingResponseDto(
            entitet.getId(),
            forespørselUuid,
            new AktørIdDto(entitet.getAktørId().getAktørId()),
            KodeverkMapper.mapYtelsetype(entitet.getYtelsetype()),
            new ArbeidsgiverDto(entitet.getArbeidsgiverIdent()),
            new SendInntektsmeldingRequestDto.KontaktpersonRequestDto(entitet.getKontaktperson().getNavn(), entitet.getKontaktperson().getTelefonnummer()),
            entitet.getStartDato(),
            entitet.getMånedInntekt(),
            entitet.getOpprettetTidspunkt(),
            refusjoner,
            bortfalteNaturalytelser,
            endringsårsaker
            );
    }

    private static List<SendInntektsmeldingRequestDto.Refusjon> mapRefusjonerTilDto(InntektsmeldingEntitet entitet) {
        List<SendInntektsmeldingRequestDto.Refusjon> refusjoner = new ArrayList<>();
        if (entitet.getMånedRefusjon() != null) {
            refusjoner.add(new SendInntektsmeldingRequestDto.Refusjon(entitet.getStartDato(), entitet.getMånedRefusjon()));
        }
        // Frontend forventer at opphørsdato mappes til en liste der fom = første dag uten refusjon, må derfor legge på en dag.
        if (entitet.getOpphørsdatoRefusjon() != null && !entitet.getOpphørsdatoRefusjon().equals(Tid.TIDENES_ENDE)) {
            refusjoner.add(new SendInntektsmeldingRequestDto.Refusjon(entitet.getOpphørsdatoRefusjon().plusDays(1), BigDecimal.ZERO));
        }
        entitet.getRefusjonsendringer().stream().map(i -> new SendInntektsmeldingRequestDto.Refusjon(i.getFom(), i.getRefusjonPrMnd())).forEach(refusjoner::add);
        return refusjoner.stream().sorted(Comparator.comparing(SendInntektsmeldingRequestDto.Refusjon::fom)).toList();
    }

    private static Optional<LocalDate> finnOpphørsdato(List<SendInntektsmeldingRequestDto.Refusjon> refusjonsendringRequestDtos,
                                                       LocalDate startdato) {
        var sisteEndring = finnSisteEndring(refusjonsendringRequestDtos, startdato);
        // Hvis siste endring setter refusjon til 0 er det å regne som opphør av refusjon,
        // setter dagen før denne endringen som opphørsdato
        return sisteEndring.filter(en -> en.beløp().compareTo(BigDecimal.ZERO) == 0).map(sr -> sr.fom().minusDays(1));
    }

    private static Optional<SendInntektsmeldingRequestDto.Refusjon> finnSisteEndring(List<SendInntektsmeldingRequestDto.Refusjon> refusjonsendringRequestDtos,
                                                                           LocalDate startdato) {
        return refusjonsendringRequestDtos.stream()
            .filter(r -> !r.fom().equals(startdato))
            .max(Comparator.comparing(SendInntektsmeldingRequestDto.Refusjon::fom));
    }

    private static List<RefusjonsendringEntitet> mapRefusjonsendringer(LocalDate startdato, LocalDate opphørsdato, List<SendInntektsmeldingRequestDto.Refusjon> refusjonsendringRequestDto) {
        // Opphør og start ligger på egne felter, så disse skal ikke mappes som endringer.
        // Merk at opphørsdato er dagen før endring som opphører refusjon, derfor må vi legge til en dag.
        return refusjonsendringRequestDto.stream()
            .filter(r -> !r.fom().equals(startdato))
            .filter(r -> !r.fom().equals(opphørsdato.plusDays(1)))
            .map(dto -> new RefusjonsendringEntitet(dto.fom(), dto.beløp()))
            .toList();
    }

    private static List<BortaltNaturalytelseEntitet> mapBortfalteNaturalytelser(List<SendInntektsmeldingRequestDto.BortfaltNaturalytelseRequestDto> dto) {
        return dto.stream()
            .map(d -> new BortaltNaturalytelseEntitet.Builder().medPeriode(d.fom(), d.tom() != null ? d.tom() : Tid.TIDENES_ENDE )
                .medMånedBeløp(d.beløp())
                .medType(KodeverkMapper.mapNaturalytelseTilEntitet(d.naturalytelsetype()))
                .build())
            .toList();
    }

    private static KontaktpersonEntitet mapKontaktPerson(SendInntektsmeldingRequestDto dto) {
        return new KontaktpersonEntitet(dto.kontaktperson().navn(), dto.kontaktperson().telefonnummer());
    }
}
