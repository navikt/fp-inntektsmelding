package no.nav.foreldrepenger.inntektsmelding.inntektsmelding.task;

import java.util.List;

import jakarta.xml.bind.JAXBElement;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.utils.OrganisasjonsnummerValidator;
import no.nav.foreldrepenger.inntektsmelding.utils.mapper.NaturalYtelseMapper;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Arbeidsforhold;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Arbeidsgiver;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Avsendersystem;
import no.seres.xsd.nav.inntektsmelding_m._20181211.EndringIRefusjon;
import no.seres.xsd.nav.inntektsmelding_m._20181211.EndringIRefusjonsListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.GjenopptakelseNaturalytelseListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Inntekt;
import no.seres.xsd.nav.inntektsmelding_m._20181211.InntektsmeldingM;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Kontaktinformasjon;
import no.seres.xsd.nav.inntektsmelding_m._20181211.NaturalytelseDetaljer;
import no.seres.xsd.nav.inntektsmelding_m._20181211.ObjectFactory;
import no.seres.xsd.nav.inntektsmelding_m._20181211.OpphoerAvNaturalytelseListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Refusjon;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Skjemainnhold;

class InntektsmeldingXMLMapper {

    private static final ObjectFactory of = new ObjectFactory();

    private InntektsmeldingXMLMapper() {
        // Hide constructor for static util class
    }

    static InntektsmeldingM map(InntektsmeldingDto inntektsmelding, PersonIdent søkerIdent) {

        var skjemainnhold = new Skjemainnhold();

        var arbeidsgiverIdent = inntektsmelding.getArbeidsgiver().orgnr();
        var arbeidsgiver = new Arbeidsgiver();
        arbeidsgiver.setVirksomhetsnummer(arbeidsgiverIdent);
        arbeidsgiver.setKontaktinformasjon(lagKontaktperson(inntektsmelding));
        var agOrg = of.createSkjemainnholdArbeidsgiver(arbeidsgiver);
        skjemainnhold.setArbeidsgiver(agOrg);

        skjemainnhold.setArbeidsforhold(lagArbeidsforholdXml(inntektsmelding));
        skjemainnhold.setArbeidstakerFnr(søkerIdent.getIdent());

        skjemainnhold.setAarsakTilInnsending("Ny");
        skjemainnhold.setAvsendersystem(lagAvsendersysem(inntektsmelding));

        skjemainnhold.setYtelse(mapTilYtelsetype(Ytelsetype.valueOf(inntektsmelding.getYtelse().name())));
        settStartdatoHvisFP(skjemainnhold, inntektsmelding);
        skjemainnhold.setRefusjon(lagRefusjonXml(inntektsmelding));

        var naturalYtelser = NaturalYtelseMapper.mapNaturalYtelserFraDto(inntektsmelding.getBortfaltNaturalytelsePerioder());
        skjemainnhold.setOpphoerAvNaturalytelseListe(lagBortfaltNaturalytelse(naturalYtelser));
        skjemainnhold.setGjenopptakelseNaturalytelseListe(lagGjennopptattNaturalytelse(naturalYtelser));

        var imXml = new InntektsmeldingM();
        imXml.setSkjemainnhold(skjemainnhold);
        return imXml;
    }

    private static void settStartdatoHvisFP(Skjemainnhold skjemainnhold, InntektsmeldingDto inntektsmelding) {
        if (Ytelsetype.FORELDREPENGER.equals(inntektsmelding.getYtelse())) {
            skjemainnhold.setStartdatoForeldrepengeperiode(of.createSkjemainnholdStartdatoForeldrepengeperiode(inntektsmelding.getStartdato()));
        }
    }

    // TODO Vi bør ta en diskusjon på hva denne skal være
    private static Avsendersystem lagAvsendersysem(InntektsmeldingDto inntektsmelding) {
        var as = new Avsendersystem();
        as.setSystemnavn(mapTilSystem(inntektsmelding.getKildesystem()).name());
        as.setSystemversjon("1.0");
        as.setInnsendingstidspunkt(of.createAvsendersystemInnsendingstidspunkt(inntektsmelding.getInnsendtTidspunkt()));
        return as;
    }

    private static Systemnavn mapTilSystem(Kildesystem kildesystem) {
        return switch (kildesystem) {
            case FPSAK -> Systemnavn.OVERSTYRING_FPSAK;
            case ARBEIDSGIVERPORTAL -> Systemnavn.NAV_NO;
            case API -> Systemnavn.HR_SYSTEM_API;
        };
    }

    private static JAXBElement<GjenopptakelseNaturalytelseListe> lagGjennopptattNaturalytelse(List<NaturalYtelseMapper.NaturalYtelse> ytelser) {
        var gjennoptakelseListeObjekt = new GjenopptakelseNaturalytelseListe();
        var gjennoptakelseListe = gjennoptakelseListeObjekt.getNaturalytelseDetaljer();
        ytelser.stream()
            .filter(by -> !by.bortfallt())
            .forEach(tilkommetNat -> gjennoptakelseListe.add(opprettNaturalYtelseDetaljer(tilkommetNat)));
        return of.createSkjemainnholdGjenopptakelseNaturalytelseListe(gjennoptakelseListeObjekt);
    }

    private static JAXBElement<OpphoerAvNaturalytelseListe> lagBortfaltNaturalytelse(List<NaturalYtelseMapper.NaturalYtelse> ytelser) {
        var opphørListeObjekt = new OpphoerAvNaturalytelseListe();
        var opphørListe = opphørListeObjekt.getOpphoerAvNaturalytelse();
        ytelser.stream()
            .filter(NaturalYtelseMapper.NaturalYtelse::bortfallt)
            .forEach(nat -> opphørListe.add(opprettNaturalYtelseDetaljer(nat)));
        return of.createSkjemainnholdOpphoerAvNaturalytelseListe(opphørListeObjekt);
    }

    private static NaturalytelseDetaljer opprettNaturalYtelseDetaljer(NaturalYtelseMapper.NaturalYtelse naturalYtelse) {
        var nd = new NaturalytelseDetaljer();
        nd.setFom(of.createNaturalytelseDetaljerFom(naturalYtelse.fom()));
        nd.setBeloepPrMnd(of.createNaturalytelseDetaljerBeloepPrMnd(naturalYtelse.beløp()));
        nd.setNaturalytelseType(of.createNaturalytelseDetaljerNaturalytelseType(mapTilNaturalytelsetype(naturalYtelse.type())));
        return nd;
    }

    private static JAXBElement<Refusjon> lagRefusjonXml(InntektsmeldingDto inntektsmelding) {
        var refusjon = new Refusjon();
        if (inntektsmelding.getMånedRefusjon() != null) {
            refusjon.setRefusjonsbeloepPrMnd(of.createRefusjonRefusjonsbeloepPrMnd(inntektsmelding.getMånedRefusjon()));
        }
        if (inntektsmelding.getOpphørsdatoRefusjon() != null) {
            refusjon.setRefusjonsopphoersdato(of.createRefusjonRefusjonsopphoersdato(inntektsmelding.getOpphørsdatoRefusjon()));
        }
        var endringListe = new EndringIRefusjonsListe();
        var liste = endringListe.getEndringIRefusjon();
        inntektsmelding.getSøkteRefusjonsperioder().stream().map(rp -> {
            var endring = new EndringIRefusjon();
            endring.setEndringsdato(of.createEndringIRefusjonEndringsdato(rp.fom()));
            endring.setRefusjonsbeloepPrMnd(of.createEndringIRefusjonRefusjonsbeloepPrMnd(rp.beløp()));
            return endring;
        }).forEach(liste::add);
        refusjon.setEndringIRefusjonListe(of.createRefusjonEndringIRefusjonListe(endringListe));
        return of.createSkjemainnholdRefusjon(refusjon);
    }

    private static JAXBElement<Arbeidsforhold> lagArbeidsforholdXml(InntektsmeldingDto inntektsmelding) {
        var arbeidsforhold = new Arbeidsforhold();

        // Inntekt
        var inntektBeløp = of.createInntektBeloep(inntektsmelding.getMånedInntekt());
        var inntekt = new Inntekt();
        inntekt.setBeloep(inntektBeløp);
        // TODO Endringsarsak kan være enten "Tariffendring" eller "FeilInntekt", skal vi bruke disse?
        var inntektSkjemaVerdi = of.createArbeidsforholdBeregnetInntekt(inntekt);
        arbeidsforhold.setBeregnetInntekt(inntektSkjemaVerdi);

        // Startdato
        arbeidsforhold.setFoersteFravaersdag(of.createArbeidsforholdFoersteFravaersdag(inntektsmelding.getStartdato()));
        return of.createSkjemainnholdArbeidsforhold(arbeidsforhold);
    }

    private static Kontaktinformasjon lagKontaktperson(InntektsmeldingDto inntektsmelding) {
        var ki = new Kontaktinformasjon();
        // Ved overstyring av inntektsmelding setter vi saksbehandlers informasjon her
        if (Kildesystem.FPSAK.equals(inntektsmelding.getKildesystem())) {
            ki.setTelefonnummer(inntektsmelding.getOpprettetAv());
            ki.setKontaktinformasjonNavn(inntektsmelding.getOpprettetAv());
        } else {
            var kontaktPerson = inntektsmelding.getKontaktperson();
            ki.setTelefonnummer(kontaktPerson.telefonnummer());
            ki.setKontaktinformasjonNavn(kontaktPerson.navn());
        }
        return ki;
    }

    private static String mapTilYtelsetype(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> "Foreldrepenger";
            case SVANGERSKAPSPENGER -> "Svangerskapspenger";
        };
    }

    private static String mapTilNaturalytelsetype(NaturalytelseType naturalytelsetype) {
        return switch (naturalytelsetype) {
            case ELEKTRISK_KOMMUNIKASJON -> "elektroniskKommunikasjon";
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS -> "aksjerGrunnfondsbevisTilUnderkurs";
            case LOSJI -> "losji";
            case KOST_DOEGN -> "kostDoegn";
            case BESØKSREISER_HJEMMET_ANNET -> "besoeksreiserHjemmetAnnet";
            case KOSTBESPARELSE_I_HJEMMET -> "kostbesparelseIHjemmet";
            case RENTEFORDEL_LÅN -> "rentefordelLaan";
            case BIL -> "bil";
            case KOST_DAGER -> "kostDager";
            case BOLIG -> "bolig";
            case SKATTEPLIKTIG_DEL_FORSIKRINGER -> "skattepliktigDelForsikringer";
            case FRI_TRANSPORT -> "friTransport";
            case OPSJONER -> "opsjoner";
            case TILSKUDD_BARNEHAGEPLASS -> "tilskuddBarnehageplass";
            case ANNET -> "annet";
            case BEDRIFTSBARNEHAGEPLASS -> "bedriftsbarnehageplass";
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER -> "yrkebilTjenestligbehovKilometer";
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS -> "yrkebilTjenestligbehovListepris";
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING -> "innbetalingTilUtenlandskPensjonsordning";
        };
    }

    // OBS OBS: Disse sendes inn i XML og skal ikke omdøpes!
    enum Systemnavn {
        OVERSTYRING_FPSAK,
        NAV_NO,
        HR_SYSTEM_API
    }
}
