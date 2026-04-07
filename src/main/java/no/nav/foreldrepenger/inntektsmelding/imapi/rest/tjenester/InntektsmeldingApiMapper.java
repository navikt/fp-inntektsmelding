package no.nav.foreldrepenger.inntektsmelding.imapi.rest.tjenester;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.BortfaltNaturalytelse;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.SendInntektsmeldingRequest;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.SøktRefusjon;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Endringsårsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.konfig.Tid;

public class InntektsmeldingApiMapper {
    private InntektsmeldingApiMapper() {
        // Mapper klasse, skal ikke instansieres
    }

    public static InntektsmeldingDto mapTilDto(SendInntektsmeldingRequest eksternRequest, AktørId aktørId) {
        // LPS sender kun inn en liste med refusjonsfra-datoer. Vi utleder startdato og opphørsdato utifra denne lista.
        var refusjonPrMnd = finnFørsteRefusjon(eksternRequest.refusjon(), eksternRequest.startdato()).orElse(null);
        var opphørsdato = refusjonPrMnd == null ? null : finnOpphørsdato(eksternRequest.refusjon(), eksternRequest.startdato()).orElse(Tid.TIDENES_ENDE);

        return InntektsmeldingDto.builder()
            .medArbeidsgiver(Arbeidsgiver.fra(eksternRequest.organisasjonsnummer().orgnr()))
            .medAktørId(aktørId)
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medStartdato(eksternRequest.startdato())
            .medYtelse(mapYtelsetype(eksternRequest.ytelseType()))
            .medKontaktperson(mapKontaktperson(eksternRequest))
            .medAvsenderSystem(mapAvsenderSystem(eksternRequest))
            .medInntekt(eksternRequest.inntekt())
            .medMånedRefusjon(refusjonPrMnd)
            .medOpphørsdatoRefusjon(opphørsdato)
            .medEndringAvInntektÅrsaker(mapEndringsårsaker(eksternRequest.endringAvInntektÅrsaker()))
            .medBortfaltNaturalytelsePerioder(mapBortfalteNaturalytelser(eksternRequest.bortfaltNaturalytelsePerioder()))
            .medSøkteRefusjonsperioder(mapRefusjonsendringer(eksternRequest.startdato(), opphørsdato, eksternRequest.refusjon()))
            .build();
    }

    private static Optional<BigDecimal> finnFørsteRefusjon(List<SøktRefusjon> refusjonListe, LocalDate startdato) {
        if (refusjonListe.isEmpty()) {
            return Optional.empty();
        }
        var refusjonPåStartdato = refusjonListe.stream().filter(r -> r.fom().equals(startdato)).toList();
        if (refusjonPåStartdato.size() != 1) {
            throw new IllegalStateException("Forventer kun 1 refusjon som starter på startdato, fant " + refusjonPåStartdato.size());
        }
        return Optional.of(refusjonPåStartdato.getFirst().beløp());
    }

    private static Optional<LocalDate> finnOpphørsdato(List<SøktRefusjon> refusjonListe,
                                                       LocalDate startdato) {
        var sisteEndring = finnSisteEndring(refusjonListe, startdato);
        // Hvis siste endring setter refusjon til 0 er det å regne som opphør av refusjon,
        // setter dagen før denne endringen som opphørsdato
        return sisteEndring.filter(en -> en.beløp().compareTo(BigDecimal.ZERO) == 0).map(sr -> sr.fom().minusDays(1));
    }

    private static Optional<SøktRefusjon> finnSisteEndring(List<SøktRefusjon> refusjonListe,
                                                           LocalDate startdato) {
        return refusjonListe.stream()
            .filter(r -> !r.fom().equals(startdato))
            .max(Comparator.comparing(SøktRefusjon::fom));
    }

    private static InntektsmeldingDto.Kontaktperson mapKontaktperson(SendInntektsmeldingRequest eksternRequest) {
        return new InntektsmeldingDto.Kontaktperson(
            eksternRequest.kontaktperson().telefonnummer(),
            eksternRequest.kontaktperson().navn()
        );
    }

    private static InntektsmeldingDto.AvsenderSystem mapAvsenderSystem(SendInntektsmeldingRequest eksternRequest) {
        return new InntektsmeldingDto.AvsenderSystem(
            eksternRequest.avsenderSystem().systemNavn(),
            eksternRequest.avsenderSystem().systemVersjon()
        );
    }

    private static List<InntektsmeldingDto.Endringsårsaker> mapEndringsårsaker(
        List<no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.Endringsårsaker> endringsårsaker) {
        return endringsårsaker.stream().map(InntektsmeldingApiMapper::mapEndringsårsaker).toList();
    }

    private static InntektsmeldingDto.Endringsårsaker mapEndringsårsaker(
        no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.Endringsårsaker e) {
        return new InntektsmeldingDto.Endringsårsaker(
            mapEndringsårsak(e.årsak()),
            e.fom(),
            e.tom(),
            e.bleKjentFom()
        );
    }

    private static List<InntektsmeldingDto.BortfaltNaturalytelse> mapBortfalteNaturalytelser(List<BortfaltNaturalytelse> bortfalteNaturalytelser) {
        return bortfalteNaturalytelser.stream()
            .map(d -> new InntektsmeldingDto.BortfaltNaturalytelse(
                d.fom(),
                d.tom() != null ? d.tom() : Tid.TIDENES_ENDE,
                mapNaturalytelse(d.naturalytelsetype()),
                d.beløp()))
            .toList();
    }

    private static List<InntektsmeldingDto.SøktRefusjon> mapRefusjonsendringer(LocalDate startdato,
                                                                               LocalDate opphørsdato,
                                                                               List<SøktRefusjon> refusjonListe) {
        // Opphør og start er allerede mappet til egne felter, så de må utelukkes her
        // Merk at opphørsdato er dagen før endring som opphører refusjon, derfor må vi legge til en dag.
        return refusjonListe.stream()
            .filter(r -> !r.fom().equals(startdato))
            .filter(r -> opphørsdato == null || !r.fom().equals(opphørsdato.plusDays(1)))
            .map(dto -> new InntektsmeldingDto.SøktRefusjon(dto.fom(), dto.beløp()))
            .toList();
    }

    // ---- Enum-mapping fra imapi kontrakt til domene ----

    private static Ytelsetype mapYtelsetype(no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.YtelseType eksternType) {
        return switch (eksternType) {
            case FORELDREPENGER -> Ytelsetype.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> Ytelsetype.SVANGERSKAPSPENGER;
        };
    }

    private static Endringsårsak mapEndringsårsak(no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.Endringsårsak eksternÅrsak) {
        return switch (eksternÅrsak) {
            case PERMITTERING -> Endringsårsak.PERMITTERING;
            case NY_STILLING -> Endringsårsak.NY_STILLING;
            case NY_STILLINGSPROSENT -> Endringsårsak.NY_STILLINGSPROSENT;
            case SYKEFRAVÆR -> Endringsårsak.SYKEFRAVÆR;
            case BONUS -> Endringsårsak.BONUS;
            case FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER -> Endringsårsak.FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER;
            case NYANSATT -> Endringsårsak.NYANSATT;
            case MANGELFULL_RAPPORTERING_AORDNING -> Endringsårsak.MANGELFULL_RAPPORTERING_AORDNING;
            case INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING -> Endringsårsak.INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING;
            case TARIFFENDRING -> Endringsårsak.TARIFFENDRING;
            case FERIE -> Endringsårsak.FERIE;
            case VARIG_LØNNSENDRING -> Endringsårsak.VARIG_LØNNSENDRING;
            case PERMISJON -> Endringsårsak.PERMISJON;
        };
    }

    private static NaturalytelseType mapNaturalytelse(no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.Naturalytelsetype eksternType) {
        return switch (eksternType) {
            case ELEKTRISK_KOMMUNIKASJON -> NaturalytelseType.ELEKTRISK_KOMMUNIKASJON;
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS -> NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS;
            case LOSJI -> NaturalytelseType.LOSJI;
            case KOST_DOEGN -> NaturalytelseType.KOST_DOEGN;
            case BESØKSREISER_HJEMMET_ANNET -> NaturalytelseType.BESØKSREISER_HJEMMET_ANNET;
            case KOSTBESPARELSE_I_HJEMMET -> NaturalytelseType.KOSTBESPARELSE_I_HJEMMET;
            case RENTEFORDEL_LÅN -> NaturalytelseType.RENTEFORDEL_LÅN;
            case BIL -> NaturalytelseType.BIL;
            case KOST_DAGER -> NaturalytelseType.KOST_DAGER;
            case BOLIG -> NaturalytelseType.BOLIG;
            case SKATTEPLIKTIG_DEL_FORSIKRINGER -> NaturalytelseType.SKATTEPLIKTIG_DEL_FORSIKRINGER;
            case FRI_TRANSPORT -> NaturalytelseType.FRI_TRANSPORT;
            case OPSJONER -> NaturalytelseType.OPSJONER;
            case TILSKUDD_BARNEHAGEPLASS -> NaturalytelseType.TILSKUDD_BARNEHAGEPLASS;
            case ANNET -> NaturalytelseType.ANNET;
            case BEDRIFTSBARNEHAGEPLASS -> NaturalytelseType.BEDRIFTSBARNEHAGEPLASS;
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER -> NaturalytelseType.YRKEBIL_TJENESTLIGBEHOV_KILOMETER;
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS -> NaturalytelseType.YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS;
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING -> NaturalytelseType.INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING;
        };
    }
}
