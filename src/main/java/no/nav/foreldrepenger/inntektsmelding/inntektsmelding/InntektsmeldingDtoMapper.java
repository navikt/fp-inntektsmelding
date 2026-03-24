package no.nav.foreldrepenger.inntektsmelding.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.BortaltNaturalytelseEntitet;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.EndringsårsakEntitet;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.InntektsmeldingEntitet;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.KontaktpersonEntitet;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.LpsSystemInfoEntitet;
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
            .medId(entitet.getId()) // TODO Fjern denne når frontend ikke lenger bruker den
            .medInntektsmeldingUuid(entitet.getUuid())
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
            .medAvsenderSystem(entitet.getLpsSystem() != null ? new InntektsmeldingDto.AvsenderSystem(entitet.getLpsSystem().getNavn(), entitet.getLpsSystem().getVersjon()) : null)
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
    var builder = InntektsmeldingEntitet.builder()
        .medAktørId(new AktørIdEntitet(inntektsmeldingDto.getAktørId().getAktørId()))
        .medYtelsetype(inntektsmeldingDto.getYtelse())
        .medArbeidsgiverIdent(inntektsmeldingDto.getArbeidsgiver().orgnr())
        .medStartDato(inntektsmeldingDto.getStartdato())
        .medMånedInntekt(inntektsmeldingDto.getMånedInntekt() != null ? inntektsmeldingDto.getMånedInntekt() : BigDecimal.ZERO)
        .medMånedRefusjon(inntektsmeldingDto.getMånedRefusjon())
        .medRefusjonOpphørsdato(inntektsmeldingDto.getOpphørsdatoRefusjon())
        .medOpprettetAv(inntektsmeldingDto.getOpprettetAv())
        .medKildesystem(inntektsmeldingDto.getKildesystem())
        .medRefusjonsendringer(mapRefusjonsendringer(inntektsmeldingDto.getStartdato(), inntektsmeldingDto.getOpphørsdatoRefusjon(), inntektsmeldingDto.getSøkteRefusjonsperioder()))
        .medEndringsårsaker(mapEndringsårsaker(inntektsmeldingDto.getEndringAvInntektÅrsaker()))
        .medBortfaltNaturalytelser(mapBortfaltNaturalytelser(inntektsmeldingDto.getBortfaltNaturalytelsePerioder()));

    mapKontaktperson(inntektsmeldingDto.getKontaktperson()).ifPresent(builder::medKontaktperson);

    return builder.build();
}

private static Optional<KontaktpersonEntitet> mapKontaktperson(InntektsmeldingDto.Kontaktperson kontaktperson) {
    if (kontaktperson == null) {
        return Optional.empty();
    }
    if (kontaktperson.navn() == null || kontaktperson.telefonnummer() == null) {
        throw new IllegalArgumentException("Kontaktperson må ha både navn og telefonnummer");
    }
    return Optional.of(new KontaktpersonEntitet(kontaktperson.navn(), kontaktperson.telefonnummer()));
}

    private static LpsSystemInfoEntitet mapLpsSystemInformasjon(InntektsmeldingDto.AvsenderSystem avsenderSystem) {
        return LpsSystemInfoEntitet.builder().medNavn(avsenderSystem.navn()).medVersjon(avsenderSystem.versjon()).build();
    }

    private static List<RefusjonsendringEntitet> mapRefusjonsendringer(LocalDate startdato,
                                                                       LocalDate opphørsdato,
                                                                       List<InntektsmeldingDto.SøktRefusjon> refusjonsendringRequestDto) {
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

