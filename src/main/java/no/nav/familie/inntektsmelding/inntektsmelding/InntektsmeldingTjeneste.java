package no.nav.familie.inntektsmelding.inntektsmelding;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;

@Dependent
public class InntektsmeldingTjeneste {

    private InntektsmeldingRepository inntektsmeldingRepository;

    InntektsmeldingTjeneste() {
        // CDI proxy
    }

    @Inject
    public InntektsmeldingTjeneste(InntektsmeldingRepository inntektsmeldingRepository) {
        this.inntektsmeldingRepository = inntektsmeldingRepository;
    }

    public InntektsmeldingDto hentInntektsmelding(long inntektsmeldingId) {
        return Optional.of(inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingId)).map(this::mapFraEntitet).orElseThrow();
    }

    public InntektsmeldingDto hentInntektsmelding(UUID inntektsmeldingUuid) {
        return inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingUuid).map(this::mapFraEntitet).orElse(null);
    }

    private InntektsmeldingDto mapFraEntitet(InntektsmeldingEntitet entitet) {
        return InntektsmeldingDto.builder()
            .medInntektsmeldingUuid(entitet.getUuid().orElseThrow()) // siden vi søker med uuid, så skal denne alltid være satt
            .medAktørId(entitet.getAktørId().getAktørId())
            .medYtelse(InntektsmeldingDto.Ytelse.valueOf(entitet.getYtelsetype().name()))
            .medArbeidsgiver(new InntektsmeldingDto.Arbeidsgiver(entitet.getArbeidsgiverIdent()))
            .medKontaktperson(mapKontaktperson(entitet))
            .medStartdato(entitet.getStartDato())
            .medInntekt(entitet.getMånedInntekt())
            .medMånedRefusjon(entitet.getMånedRefusjon())
            .medOpphørsdatoRefusjon(entitet.getOpphørsdatoRefusjon())
            .medInnsendtTidspunkt(entitet.getOpprettetTidspunkt())
            .medSøkteRefusjonsperioder(entitet.getRefusjonsendringer().stream().map(InntektsmeldingTjeneste::mapRefusjonsendring).toList())
            .medBortfaltNaturalytelsePerioder(entitet.getBorfalteNaturalYtelser().stream().map(InntektsmeldingTjeneste::mapBortfaltNaturalytelse).toList())
            .medEndringAvInntektÅrsaker(entitet.getEndringsårsaker().stream().map(InntektsmeldingTjeneste::mapEndringsårsak).toList())
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
            InntektsmeldingDto.Naturalytelsetype.valueOf(bortfalt.getType().name()),
            bortfalt.getMånedBeløp()
        );
    }

    static InntektsmeldingDto.Endringsårsaker mapEndringsårsak(EndringsårsakEntitet endringsårsak) {
        return new InntektsmeldingDto.Endringsårsaker(
            no.nav.familie.inntektsmelding.inntektsmelding.rest.kontrakt.Endringsårsak.valueOf(endringsårsak.getÅrsak().name()),
            endringsårsak.getFom().orElse(null),
            endringsårsak.getTom().orElse(null),
            endringsårsak.getBleKjentFom().orElse(null)
        );
    }

}
