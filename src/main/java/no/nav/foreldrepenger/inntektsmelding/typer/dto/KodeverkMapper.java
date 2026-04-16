package no.nav.foreldrepenger.inntektsmelding.typer.dto;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ArbeidsgiverinitiertÅrsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.EndringsårsakType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

public class KodeverkMapper {

    private KodeverkMapper() {
        // Skjuler default konstruktør
    }

    public static NaturalytelseType mapNaturalytelseTilDomene(NaturalytelsetypeDto dto) {
        return switch (dto) {
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

    public static Ytelsetype mapYtelsetype(YtelseTypeDto dto) {
        return switch (dto) {
            case FORELDREPENGER -> Ytelsetype.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> Ytelsetype.SVANGERSKAPSPENGER;
        };
    }

    public static YtelseTypeDto mapYtelsetype(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> YtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseTypeDto.SVANGERSKAPSPENGER;
        };
    }

    public static EndringsårsakType mapEndringsårsak(EndringsårsakDto årsak) {
        return switch (årsak) {
            case PERMITTERING -> EndringsårsakType.PERMITTERING;
            case NY_STILLING -> EndringsårsakType.NY_STILLING;
            case NY_STILLINGSPROSENT -> EndringsårsakType.NY_STILLINGSPROSENT;
            case SYKEFRAVÆR -> EndringsårsakType.SYKEFRAVÆR;
            case BONUS -> EndringsårsakType.BONUS;
            case FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER -> EndringsårsakType.FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER;
            case NYANSATT -> EndringsårsakType.NYANSATT;
            case MANGELFULL_RAPPORTERING_AORDNING -> EndringsårsakType.MANGELFULL_RAPPORTERING_AORDNING;
            case INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING -> EndringsårsakType.INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING;
            case TARIFFENDRING -> EndringsårsakType.TARIFFENDRING;
            case FERIE -> EndringsårsakType.FERIE;
            case VARIG_LØNNSENDRING -> EndringsårsakType.VARIG_LØNNSENDRING;
            case PERMISJON -> EndringsårsakType.PERMISJON;
        };
    }

    public static EndringsårsakDto mapEndringsårsak(EndringsårsakType årsak) {
        return switch (årsak) {
            case PERMITTERING -> EndringsårsakDto.PERMITTERING;
            case NY_STILLING -> EndringsårsakDto.NY_STILLING;
            case NY_STILLINGSPROSENT -> EndringsårsakDto.NY_STILLINGSPROSENT;
            case SYKEFRAVÆR -> EndringsårsakDto.SYKEFRAVÆR;
            case BONUS -> EndringsårsakDto.BONUS;
            case FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER -> EndringsårsakDto.FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER;
            case NYANSATT -> EndringsårsakDto.NYANSATT;
            case MANGELFULL_RAPPORTERING_AORDNING -> EndringsårsakDto.MANGELFULL_RAPPORTERING_AORDNING;
            case INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING -> EndringsårsakDto.INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING;
            case TARIFFENDRING -> EndringsårsakDto.TARIFFENDRING;
            case FERIE -> EndringsårsakDto.FERIE;
            case VARIG_LØNNSENDRING -> EndringsårsakDto.VARIG_LØNNSENDRING;
            case PERMISJON -> EndringsårsakDto.PERMISJON;
        };
    }

    public static ForespørselStatusDto mapForespørselStatus(ForespørselStatus forespørselStatus) {
        return switch (forespørselStatus) {
            case UNDER_BEHANDLING -> ForespørselStatusDto.UNDER_BEHANDLING;
            case FERDIG -> ForespørselStatusDto.FERDIG;
            case UTGÅTT -> ForespørselStatusDto.UTGÅTT;
        };
    }

    public static ForespørselStatus mapForespørselStatus(ForespørselStatusDto forespørselStatus) {
        return switch (forespørselStatus) {
            case UNDER_BEHANDLING -> ForespørselStatus.UNDER_BEHANDLING;
            case FERDIG -> ForespørselStatus.FERDIG;
            case UTGÅTT -> ForespørselStatus.UTGÅTT;
        };
    }

    public static ArbeidsgiverinitiertÅrsak mapArbeidsgiverinitiertÅrsak(ArbeidsgiverinitiertÅrsakDto arbeidsgiverinitiertÅrsakDto) {
        return switch (arbeidsgiverinitiertÅrsakDto) {
            case NYANSATT -> ArbeidsgiverinitiertÅrsak.NYANSATT;
            case UREGISTRERT ->  ArbeidsgiverinitiertÅrsak.UREGISTRERT;
            case null -> throw new NullPointerException("Mangler årsak for arbeidsgiverinitiert inntektsmelding");
        };
    }

}
