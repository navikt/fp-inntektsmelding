package no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen;

import static no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterDatoForLister;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.utils.mapper.NaturalYtelseMapper;
import no.nav.vedtak.konfig.Tid;

class InntektsmeldingPdfDataMapper {

    private InntektsmeldingPdfDataMapper() {
        throw new IllegalStateException("InntektsmeldingPdfDataMapper: Utility class");
    }
    public static InntektsmeldingPdfData mapInntektsmeldingPdfData(InntektsmeldingDto inntektsmelding,
                                                                   String arbeidsgiverNavn,
                                                                   PersonInfo personInfo,
                                                                   String arbeidsgvierIdent,
                                                                   ForespørselType forespørselType) {
        var startdato = inntektsmelding.getStartdato();
        //Felles
        var imDokumentdataBuilder = new InntektsmeldingPdfData.Builder()
            .medNavn(personInfo.mapNavn())
            .medPersonnummer(personInfo.fødselsnummer().getIdent())
            .medArbeidsgiverIdent(arbeidsgvierIdent)
            .medArbeidsgiverNavn(arbeidsgiverNavn)
            .medAvsenderSystem("NAV_NO")
            .medYtelseNavn(Ytelsetype.valueOf(inntektsmelding.getYtelse().name()))
            .medKontaktperson(mapKontaktperson(inntektsmelding))
            .medOpprettetTidspunkt(inntektsmelding.getInnsendtTidspunkt())
            .medStartDato(startdato);

        if (inntektsmelding.getMånedRefusjon() != null) {
            var opphørsdato = inntektsmelding.getOpphørsdatoRefusjon() != null ? inntektsmelding.getOpphørsdatoRefusjon() : null;
            var refusjonsendringerTilPdf = mapRefusjonsendringPerioder(inntektsmelding.getSøkteRefusjonsperioder(), opphørsdato, inntektsmelding.getMånedRefusjon(), startdato);
            imDokumentdataBuilder.medRefusjonsendringer(refusjonsendringerTilPdf);
            imDokumentdataBuilder.medAntallRefusjonsperioder(refusjonsendringerTilPdf.size());
        } else {
            imDokumentdataBuilder.medAntallRefusjonsperioder(0);
        }

        //Gjelder ikke arbeidsgiverinitiert nyansatt
        if (!ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT.equals(forespørselType)) {
            imDokumentdataBuilder.medMånedInntekt(inntektsmelding.getMånedInntekt())
                .medNaturalytelser(mapNaturalYtelser(inntektsmelding.getBortfaltNaturalytelsePerioder()))
                .medIngenBortfaltNaturalytelse(erIngenBortalteNaturalYtelser(inntektsmelding.getBortfaltNaturalytelsePerioder()))
                .medIngenGjenopptattNaturalytelse(erIngenGjenopptatteNaturalYtelser(inntektsmelding.getBortfaltNaturalytelsePerioder()))
                .medEndringsårsaker(mapEndringsårsaker(inntektsmelding.getEndringAvInntektÅrsaker()));
        }

        return imDokumentdataBuilder.build();
    }

    private static List<Endringsarsak> mapEndringsårsaker(List<InntektsmeldingDto.Endringsårsaker> endringsårsaker) {
        return endringsårsaker.stream()
            .map(årsak -> new Endringsarsak(
                årsak.årsak().getBeskrivelse(),
                formaterDatoForLister(årsak.fom()),
                formaterDatoForLister(årsak.tom()),
                formaterDatoForLister(årsak.bleKjentFom())))
            .toList();
    }

    private static Kontaktperson mapKontaktperson(InntektsmeldingDto inntektsmelding) {
        if (Kildesystem.FPSAK.equals(inntektsmelding.getKildesystem())) {
            return new Kontaktperson(inntektsmelding.getOpprettetAv(), inntektsmelding.getOpprettetAv());
        } else {
            return new Kontaktperson(inntektsmelding.getKontaktperson().navn(), inntektsmelding.getKontaktperson().telefonnummer());
        }
    }

    private static boolean erIngenGjenopptatteNaturalYtelser(List<InntektsmeldingDto.BortfaltNaturalytelse> naturalYtelser) {
        return naturalYtelser.isEmpty() || naturalYtelser.stream().filter(n -> n.tom().isBefore(Tid.TIDENES_ENDE)).toList().isEmpty();
    }

    private static boolean erIngenBortalteNaturalYtelser(List<InntektsmeldingDto.BortfaltNaturalytelse> naturalYtelser) {
        return naturalYtelser.isEmpty();
    }

    private static List<NaturalYtelse> mapNaturalYtelser(List<InntektsmeldingDto.BortfaltNaturalytelse> naturalytelser) {
        return NaturalYtelseMapper.mapNaturalYtelser(naturalytelser).stream()
            .map(InntektsmeldingPdfDataMapper::opprettNaturalytelserTilBrev)
            .toList();
    }

    private static NaturalYtelse opprettNaturalytelserTilBrev(NaturalYtelseMapper.NaturalYtelse bn) {
        return new NaturalYtelse(formaterDatoForLister(bn.fom()),
            mapTypeTekst(bn.type()),
            bn.beløp(),
            bn.bortfallt());
    }

    private static String mapTypeTekst(NaturalytelseType type) {
        return switch (type) {
            case ELEKTRISK_KOMMUNIKASJON -> "Elektrisk kommunikasjon";
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS -> "Aksjer grunnfondsbevis til underkurs";
            case LOSJI -> "Losji";
            case KOST_DOEGN -> "Kostpenger døgnsats";
            case BESØKSREISER_HJEMMET_ANNET -> "Besøksreiser hjemmet annet";
            case KOSTBESPARELSE_I_HJEMMET -> "Kostbesparelser i hjemmet";
            case RENTEFORDEL_LÅN -> "Rentefordel lån";
            case BIL -> "Bil";
            case KOST_DAGER -> "Kostpenger dager";
            case BOLIG -> "Bolig";
            case SKATTEPLIKTIG_DEL_FORSIKRINGER -> "Skattepliktig del forsikringer";
            case FRI_TRANSPORT -> "Fri transport";
            case OPSJONER -> "Opsjoner";
            case TILSKUDD_BARNEHAGEPLASS -> "Tilskudd barnehageplass";
            case ANNET -> "Annet";
            case BEDRIFTSBARNEHAGEPLASS -> "Bedriftsbarnehageplass";
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER -> "Yrkesbil tjenesteligbehov kilometer";
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS -> "Yrkesbil tjenesteligbehov listepris";
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING -> "Innbetaling utenlandsk pensjonsordning";
        };
    }

    private static List<RefusjonsendringPeriode> mapRefusjonsendringPerioder(List<InntektsmeldingDto.SøktRefusjon> refusjonsendringer,
                                                                             LocalDate opphørsdato,
                                                                             BigDecimal refusjonsbeløp,
                                                                             LocalDate startdato) {
        List<RefusjonsendringPeriode> refusjonsendringerTilBrev = new ArrayList<>();

        //første perioden
        refusjonsendringerTilBrev.add(new RefusjonsendringPeriode(formaterDatoForLister(startdato), startdato, refusjonsbeløp));

        refusjonsendringerTilBrev.addAll(
            refusjonsendringer.stream().map(rpe -> new RefusjonsendringPeriode(formaterDatoForLister(rpe.fom()), rpe.fom(), rpe.beløp()))
            .toList());

        if (opphørsdato != null && !opphørsdato.equals(Tid.TIDENES_ENDE)) {
            // Da opphørsdato er siste dag med refusjon må vi legge til denne mappingen for å få det rett ut i PDF, da vi ønsker å vise når første dag uten refusjon er
            refusjonsendringerTilBrev.add(new RefusjonsendringPeriode(formaterDatoForLister(opphørsdato.plusDays(1)), opphørsdato.plusDays(1), BigDecimal.ZERO));
        }

        return refusjonsendringerTilBrev.stream()
            .sorted(Comparator.comparing(RefusjonsendringPeriode::fraDato))
            .toList();
    }
}
