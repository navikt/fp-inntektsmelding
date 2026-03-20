package no.nav.foreldrepenger.inntektsmelding.inntektsmelding;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.BortaltNaturalytelseEntitet;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.EndringsårsakEntitet;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.InntektsmeldingEntitet;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.KontaktpersonEntitet;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.RefusjonsendringEntitet;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;

public class InntektsmeldingDtoMapper {

    private InntektsmeldingDtoMapper() {
        // Skjuler default konstruktør
    }

    // ---- Entitet -> Dto ----

    public static InntektsmeldingDto mapFraEntitet(InntektsmeldingEntitet entitet) {
        return InntektsmeldingDto.builder()
            .medId(entitet.getId())
            .medInntektsmeldingUuid(entitet.getUuid().orElse(null)) // siden vi søker med uuid, så skal denne alltid være satt
            .medAktørId(AktørId.fra(entitet.getAktørId().getAktørId()))
            .medYtelse(entitet.getYtelsetype())
            .medArbeidsgiver(new Arbeidsgiver(entitet.getArbeidsgiverIdent()))
            .medKontaktperson(mapKontaktperson(entitet))
            .medStartdato(entitet.getStartDato())
            .medInntekt(entitet.getMånedInntekt())
            .medMånedRefusjon(entitet.getMånedRefusjon())
            .medOpphørsdatoRefusjon(entitet.getOpphørsdatoRefusjon())
            .medOpprettetAv(entitet.getOpprettetAv())
            .medInnsendtTidspunkt(entitet.getOpprettetTidspunkt())
            .medKildesystem(entitet.getKildesystem())
            .medSøkteRefusjonsperioder(entitet.getRefusjonsendringer().stream().map(InntektsmeldingDtoMapper::mapRefusjonsendring).toList())
            .medBortfaltNaturalytelsePerioder(entitet.getBorfalteNaturalYtelser().stream().map(InntektsmeldingDtoMapper::mapBortfaltNaturalytelse).toList())
            .medEndringAvInntektÅrsaker(entitet.getEndringsårsaker().stream().map(InntektsmeldingDtoMapper::mapEndringsårsak).toList())
            .build();
    }

    static InntektsmeldingDto.Kontaktperson mapKontaktperson(InntektsmeldingEntitet entitet) {
        var kontaktperson = entitet.getKontaktperson();
        if (kontaktperson == null) {
            return null;
        }
        return new InntektsmeldingDto.Kontaktperson(kontaktperson.getTelefonnummer(), kontaktperson.getNavn());
    }

    static InntektsmeldingDto.SøktRefusjon mapRefusjonsendring(RefusjonsendringEntitet refusjonsendring) {
        return new InntektsmeldingDto.SøktRefusjon(refusjonsendring.getFom(), refusjonsendring.getRefusjonPrMnd());
    }

    static InntektsmeldingDto.BortfaltNaturalytelse mapBortfaltNaturalytelse(BortaltNaturalytelseEntitet bortfalt) {
        return new InntektsmeldingDto.BortfaltNaturalytelse(
            bortfalt.getPeriode().getFom(),
            bortfalt.getPeriode().getTom(),
            bortfalt.getType(),
            bortfalt.getMånedBeløp()
        );
    }

    static InntektsmeldingDto.Endringsårsaker mapEndringsårsak(EndringsårsakEntitet endringsårsak) {
        return new InntektsmeldingDto.Endringsårsaker(
            endringsårsak.getÅrsak(),
            endringsårsak.getFom().orElse(null),
            endringsårsak.getTom().orElse(null),
            endringsårsak.getBleKjentFom().orElse(null)
        );
    }

    // ---- Dto -> Entitet ----

    public static InntektsmeldingEntitet mapTilEntitet(InntektsmeldingDto inntektsmeldingDto) {
        return InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet(inntektsmeldingDto.getAktørId().getAktørId()))
            .medYtelsetype(inntektsmeldingDto.getYtelse())
            .medArbeidsgiverIdent(inntektsmeldingDto.getArbeidsgiver().orgnr())
            .medKontaktperson(new KontaktpersonEntitet(inntektsmeldingDto.getKontaktperson().navn(), inntektsmeldingDto.getKontaktperson().telefonnummer()))
            .medStartDato(inntektsmeldingDto.getStartdato())
            .medMånedInntekt(inntektsmeldingDto.getMånedInntekt())
            .medMånedRefusjon(inntektsmeldingDto.getMånedRefusjon())
            .medRefusjonOpphørsdato(inntektsmeldingDto.getOpphørsdatoRefusjon())
            .medOpprettetAv(inntektsmeldingDto.getOpprettetAv())
            .medKildesystem(inntektsmeldingDto.getKildesystem())
            .medRefusjonsendringer(mapRefusjonsendringer(inntektsmeldingDto.getStartdato(), inntektsmeldingDto.getOpphørsdatoRefusjon(), inntektsmeldingDto.getSøkteRefusjonsperioder()))
            .medEndringsårsaker(mapEndringsårsaker(inntektsmeldingDto.getEndringAvInntektÅrsaker()))
            .medBortfaltNaturalytelser(mapBortfaltNaturalytelser(inntektsmeldingDto.getBortfaltNaturalytelsePerioder()))
            .build();
    }

    private static List<RefusjonsendringEntitet> mapRefusjonsendringer(LocalDate startdato, LocalDate opphørsdato, List<InntektsmeldingDto.SøktRefusjon> refusjonsendringRequestDto) {
        // Opphør og start ligger på egne felter, så disse skal ikke mappes som endringer.
        // Merk at opphørsdato er dagen før endring som opphører refusjon, derfor må vi legge til en dag.
        return refusjonsendringRequestDto.stream()
            .filter(r -> !r.fom().equals(startdato))
            .filter(r -> !r.fom().equals(opphørsdato.plusDays(1)))
            .map(dto -> new RefusjonsendringEntitet(dto.fom(), dto.beløp()))
            .toList();
    }

    private static List<EndringsårsakEntitet> mapEndringsårsaker(List<InntektsmeldingDto.Endringsårsaker> endringsårsaker) {
        return endringsårsaker.stream()
            .map(e -> EndringsårsakEntitet.builder()
                .medÅrsak(e.årsak())
                .medFom(e.fom())
                .medTom(e.tom())
                .medBleKjentFra(e.bleKjentFom())
                .build())
            .toList();
    }

    private static List<BortaltNaturalytelseEntitet> mapBortfaltNaturalytelser(List<InntektsmeldingDto.BortfaltNaturalytelse> bortfalteNaturalytelser) {
        return bortfalteNaturalytelser.stream()
            .map(b -> new BortaltNaturalytelseEntitet.Builder()
                .medPeriode(b.fom(), b.tom())
                .medMånedBeløp(b.beløp())
                .medType(b.naturalytelsetype())
                .build())
            .toList();
    }
}

