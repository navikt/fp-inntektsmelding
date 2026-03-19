package no.nav.familie.inntektsmelding.imdialog.tjenester.ekstern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.imdialog.rest.ekstern.SendInntektsmeldingEksternRequest;
import no.nav.familie.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;

public class InntektsmeldingEksternMapper {
    private InntektsmeldingEksternMapper() {
        // Mapper klasse, skal ikke instansieres
    }

    public static InntektsmeldingEntitet mapTilEntitet(SendInntektsmeldingEksternRequest eksternRequest, AktørId aktørId) {
        // LPS sender kun inn en liste med refusjonsfra-datoer. Vi utleder startdato og opphørsdato utifra denne lista.
        var refusjonPrMnd = finnFørsteRefusjon(eksternRequest.refusjon(), eksternRequest.startdato()).orElse(null);
        var opphørsdato = refusjonPrMnd == null ? null : finnOpphørsdato(eksternRequest.refusjon(), eksternRequest.startdato()).orElse(Tid.TIDENES_ENDE);
        var builder = opprettBuilderOgSettFellesFelter(eksternRequest, new AktørIdEntitet(aktørId.getAktørId()));
        return builder.medMånedInntekt(eksternRequest.inntekt())
            .medMånedRefusjon(refusjonPrMnd)
            .medRefusjonOpphørsdato(opphørsdato)
            .medEndringsårsaker(mapEndringsårsaker(eksternRequest.endringAvInntektÅrsaker()))
            .medBortfaltNaturalytelser(mapBortfalteNaturalytelser(eksternRequest.bortfaltNaturalytelsePerioder()))
            .medRefusjonsendringer(mapRefusjonsendringer(eksternRequest.startdato(), opphørsdato, eksternRequest.refusjon()))
            .build();
    }

    private static Optional<BigDecimal> finnFørsteRefusjon(List<SendInntektsmeldingEksternRequest.RefusjonRequest> refusjonListe, LocalDate startdato) {
        if (refusjonListe.isEmpty()) {
            return Optional.empty();
        }
        var refusjonPåStartdato = refusjonListe.stream().filter(r -> r.fom().equals(startdato)).toList();
        if (refusjonPåStartdato.size() != 1) {
            throw new IllegalStateException("Forventer kun 1 refusjon som starter på startdato, fant " + refusjonPåStartdato.size());
        }
        return Optional.of(refusjonPåStartdato.getFirst().beløp());
    }

    private static Optional<LocalDate> finnOpphørsdato(List<SendInntektsmeldingEksternRequest.RefusjonRequest> refusjonListe,
                                                       LocalDate startdato) {
        var sisteEndring = finnSisteEndring(refusjonListe, startdato);
        // Hvis siste endring setter refusjon til 0 er det å regne som opphør av refusjon,
        // setter dagen før denne endringen som opphørsdato
        return sisteEndring.filter(en -> en.beløp().compareTo(BigDecimal.ZERO) == 0).map(sr -> sr.fom().minusDays(1));
    }

    private static Optional<SendInntektsmeldingEksternRequest.RefusjonRequest> finnSisteEndring(List<SendInntektsmeldingEksternRequest.RefusjonRequest> refusjonListe,
                                                                                    LocalDate startdato) {
        return refusjonListe.stream()
            .filter(r -> !r.fom().equals(startdato))
            .max(Comparator.comparing(SendInntektsmeldingEksternRequest.RefusjonRequest::fom));
    }

    private static InntektsmeldingEntitet.Builder opprettBuilderOgSettFellesFelter(SendInntektsmeldingEksternRequest eksternRequest, AktørIdEntitet aktørId) {
        return InntektsmeldingEntitet.builder()
            .medAktørId(aktørId)
            .medArbeidsgiverIdent(eksternRequest.organisasjonsnummer().orgnr())
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medStartDato(eksternRequest.startdato())
            .medYtelsetype(KodeverkMapper.mapYtelseType(eksternRequest.ytelseType()))
            .medKontaktperson(mapKontaktPerson(eksternRequest))
            .medLpsSystemNavn(eksternRequest.avsenderSystem().navn())
            .medLpsSystemVersjon(eksternRequest.avsenderSystem().versjon());
    }

    private static KontaktpersonEntitet mapKontaktPerson(SendInntektsmeldingEksternRequest eksternRequest) {
        return new KontaktpersonEntitet(eksternRequest.kontaktperson().navn(), eksternRequest.kontaktperson().telefonnummer());
    }

    private static List<EndringsårsakEntitet> mapEndringsårsaker(List<SendInntektsmeldingEksternRequest.EndringsårsakerRequest> endringsårsaker) {
        return endringsårsaker.stream().map(InntektsmeldingEksternMapper::mapEndringsårsak).toList();
    }

    private static EndringsårsakEntitet mapEndringsårsak(SendInntektsmeldingEksternRequest.EndringsårsakerRequest e) {
        return EndringsårsakEntitet.builder()
            .medÅrsak(KodeverkMapper.mapEndringsårsak(e.årsak()))
            .medFom(e.fom())
            .medTom(e.tom())
            .medBleKjentFra(e.bleKjentFom())
            .build();
    }

    private static List<BortaltNaturalytelseEntitet> mapBortfalteNaturalytelser(List<SendInntektsmeldingEksternRequest.BortfaltNaturalytelseRequest> bortfalteNaturalytelser) {
        return bortfalteNaturalytelser.stream()
            .map(d -> new BortaltNaturalytelseEntitet.Builder().medPeriode(d.fom(), d.tom() != null ? d.tom() : Tid.TIDENES_ENDE)
                .medMånedBeløp(d.beløp())
                .medType(KodeverkMapper.mapNaturalytelseTilEntitet(d.naturalytelsetype()))
                .build())
            .toList();
    }

    private static List<RefusjonsendringEntitet> mapRefusjonsendringer(LocalDate startdato,
                                                                       LocalDate opphørsdato,
                                                                       List<SendInntektsmeldingEksternRequest.RefusjonRequest> refusjonsendringRequestDto) {
        // Opphør og start er allerede mappet til egne felter på InntektsmeldingEniteten, så de må utelukkes her
        // Merk at opphørsdato er dagen før endring som opphører refusjon, derfor må vi legge til en dag.
        return refusjonsendringRequestDto.stream()
            .filter(r -> !r.fom().equals(startdato))
            .filter(r -> !r.fom().equals(opphørsdato.plusDays(1)))
            .map(dto -> new RefusjonsendringEntitet(dto.fom(), dto.beløp()))
            .toList();
    }
}
