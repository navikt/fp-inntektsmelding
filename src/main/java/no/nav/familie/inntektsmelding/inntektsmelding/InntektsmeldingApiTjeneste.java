package no.nav.familie.inntektsmelding.inntektsmelding;

import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;

@Dependent
public class InntektsmeldingApiTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingApiTjeneste.class);
    private InntektsmeldingRepository inntektsmeldingRepository;
    private PersonTjeneste personTjeneste;
    InntektsmeldingApiTjeneste() {
        // CDI proxy
    }
    @Inject
    public InntektsmeldingApiTjeneste(InntektsmeldingRepository inntektsmeldingRepository, PersonTjeneste personTjeneste) {
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.personTjeneste = personTjeneste;
    }
    public InntektsmeldingDto hentInntektsmelding(UUID inntektsmeldingUuid) {
        return inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingUuid).map(this::mapInntektsmelding).orElse(null);
    }

    private InntektsmeldingDto mapInntektsmelding(InntektsmeldingEntitet entitet) {
        return InntektsmeldingDto.builder()
            .medInntektsmeldingUuid(entitet.getUuid().orElse(null))
            .medFnr(hentNorskIdent(entitet.getAktørId().getAktørId()))
            .medYtelse(InntektsmeldingDto.Ytelse.valueOf(entitet.getYtelsetype().name()))
            .medArbeidsgiver(new InntektsmeldingDto.ArbeidsgiverInformasjonDto(entitet.getArbeidsgiverIdent()))
            .medKontaktperson(mapKontaktperson(entitet))
            .medStartdato(entitet.getStartDato())
            .medInntekt(entitet.getMånedInntekt())
            .medInnsendtTidspunkt(entitet.getOpprettetTidspunkt())
            .medSøkteRefusjonsperioder(entitet.getRefusjonsendringer().stream().map(InntektsmeldingApiTjeneste::mapRefusjonsendring).toList())
            .medBortfaltNaturalytelsePerioder(entitet.getBorfalteNaturalYtelser().stream().map(InntektsmeldingApiTjeneste::mapBortfaltNaturalytelse).toList())
            .medEndringAvInntektÅrsaker(entitet.getEndringsårsaker().stream().map(InntektsmeldingApiTjeneste::mapEndringsårsak).toList())
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

    private String hentNorskIdent(String aktørId) {
        return personTjeneste.finnPersonIdentForAktørId(new PersonIdent(aktørId)).map().orElse(null);
}
