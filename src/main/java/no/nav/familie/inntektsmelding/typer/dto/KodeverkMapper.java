package no.nav.familie.inntektsmelding.typer.dto;

import no.nav.familie.inntektsmelding.koder.ArbeidsgiverinitiertÅrsak;
import no.nav.familie.inntektsmelding.koder.Endringsårsak;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

public class KodeverkMapper {

    private KodeverkMapper() {
        // Skjuler default konstruktør
    }

    public static NaturalytelseType mapNaturalytelseTilEntitet(NaturalytelsetypeDto dto) {
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

    public static Endringsårsak mapEndringsårsak(EndringsårsakDto årsak) {
        return switch (årsak) {
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

    public static EndringsårsakDto mapEndringsårsak(Endringsårsak årsak) {
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

    public static ArbeidsgiverinitiertÅrsak mapArbeidsgiverinitiertÅrsak(ArbeidsgiverinitiertÅrsakDto arbeidsgiverinitiertÅrsakDto) {
        switch (arbeidsgiverinitiertÅrsakDto) {
            case NYANSATT -> {
                return ArbeidsgiverinitiertÅrsak.NYANSATT;
            }
            case UREGISTRERT -> {
                return ArbeidsgiverinitiertÅrsak.UREGISTRERT;
            }
            case null -> throw new NullPointerException("Mangler årsak for arbeidsgiverinitiert inntektsmelding");
        }
    }

}
