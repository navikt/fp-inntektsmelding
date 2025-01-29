package no.nav.familie.inntektsmelding.imdialog.task;

import java.util.List;
import java.util.Map;

import jakarta.xml.bind.JAXBElement;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerValidator;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.familie.inntektsmelding.utils.mapper.NaturalYtelseMapper;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Arbeidsforhold;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Arbeidsgiver;
import no.seres.xsd.nav.inntektsmelding_m._20181211.ArbeidsgiverPrivat;
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

public class InntektsmeldingXMLMapper {

    private static final ObjectFactory of = new ObjectFactory();

    private InntektsmeldingXMLMapper() {
        // Hide constructor for static util class
    }

    public static InntektsmeldingM map(InntektsmeldingEntitet inntektsmelding, Map<AktørIdEntitet, PersonIdent> aktørIdFnrMap) {

        var skjemainnhold = new Skjemainnhold();

        if (OrganisasjonsnummerValidator.erGyldig(inntektsmelding.getArbeidsgiverIdent())) {
            var arbeidsgiver = new Arbeidsgiver();
            arbeidsgiver.setVirksomhetsnummer(inntektsmelding.getArbeidsgiverIdent());
            arbeidsgiver.setKontaktinformasjon(lagKontaktperson(inntektsmelding));
            var agOrg = of.createSkjemainnholdArbeidsgiver(arbeidsgiver);
            skjemainnhold.setArbeidsgiver(agOrg);
        } else if (inntektsmelding.getArbeidsgiverIdent().length() == 13) {
            var arbeidsgiver = new ArbeidsgiverPrivat();
            var identArbeidsgiver = aktørIdFnrMap.get(new AktørIdEntitet(inntektsmelding.getArbeidsgiverIdent()));
            arbeidsgiver.setArbeidsgiverFnr(identArbeidsgiver.getIdent());
            arbeidsgiver.setKontaktinformasjon(lagKontaktperson(inntektsmelding));
            var agPriv = of.createSkjemainnholdArbeidsgiverPrivat(arbeidsgiver);
            skjemainnhold.setArbeidsgiverPrivat(agPriv);
        }
        skjemainnhold.setArbeidsforhold(lagArbeidsforholdXml(inntektsmelding));
        skjemainnhold.setArbeidstakerFnr(aktørIdFnrMap.get(inntektsmelding.getAktørId()).getIdent());

        skjemainnhold.setAarsakTilInnsending("Ny");
        skjemainnhold.setAvsendersystem(lagAvsendersysem(inntektsmelding));

        skjemainnhold.setYtelse(mapTilYtelsetype(inntektsmelding.getYtelsetype()));
        settStartdatoHvisFP(skjemainnhold, inntektsmelding);
        skjemainnhold.setRefusjon(lagRefusjonXml(inntektsmelding));

        var naturalYtelser = NaturalYtelseMapper.mapNaturalYtelser(inntektsmelding.getBorfalteNaturalYtelser());
        skjemainnhold.setOpphoerAvNaturalytelseListe(lagBortfaltNaturalytelse(naturalYtelser));
        skjemainnhold.setGjenopptakelseNaturalytelseListe(lagGjennopptattNaturalytelse(naturalYtelser));

        var imXml = new InntektsmeldingM();
        imXml.setSkjemainnhold(skjemainnhold);
        return imXml;
    }

    private static void settStartdatoHvisFP(Skjemainnhold skjemainnhold, InntektsmeldingEntitet inntektsmelding) {
        if (inntektsmelding.getYtelsetype().equals(Ytelsetype.FORELDREPENGER)) {
            settFPStartdato(skjemainnhold, inntektsmelding);
        }
    }

    private static void settFPStartdato(Skjemainnhold skjemainnhold, InntektsmeldingEntitet inntektsmelding) {
        skjemainnhold.setStartdatoForeldrepengeperiode(of.createSkjemainnholdStartdatoForeldrepengeperiode(inntektsmelding.getStartDato()));
    }

    // TODO Vi bør ta en diskusjon på hva denne skal være
    private static Avsendersystem lagAvsendersysem(InntektsmeldingEntitet inntektsmelding) {
        var as = new Avsendersystem();
        if (Kildesystem.FPSAK.equals(inntektsmelding.getKildesystem())) {
            as.setSystemnavn(Systemnavn.OVERSTYRING_FPSAK.name());
        } else {
            as.setSystemnavn(Systemnavn.NAV_NO.name());
        }
        as.setSystemversjon("1.0");
        as.setInnsendingstidspunkt(of.createAvsendersystemInnsendingstidspunkt(inntektsmelding.getOpprettetTidspunkt()));
        return as;
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

    private static JAXBElement<Refusjon> lagRefusjonXml(InntektsmeldingEntitet inntektsmeldingEntitet) {
        var refusjon = new Refusjon();
        if (inntektsmeldingEntitet.getMånedRefusjon() != null) {
            refusjon.setRefusjonsbeloepPrMnd(of.createRefusjonRefusjonsbeloepPrMnd(inntektsmeldingEntitet.getMånedRefusjon()));
        }
        if (inntektsmeldingEntitet.getOpphørsdatoRefusjon() != null) {
            refusjon.setRefusjonsopphoersdato(of.createRefusjonRefusjonsopphoersdato(inntektsmeldingEntitet.getOpphørsdatoRefusjon()));
        }
        var endringListe = new EndringIRefusjonsListe();
        var liste = endringListe.getEndringIRefusjon();
        inntektsmeldingEntitet.getRefusjonsendringer().stream().map(rp -> {
            var endring = new EndringIRefusjon();
            endring.setEndringsdato(of.createEndringIRefusjonEndringsdato(rp.getFom()));
            endring.setRefusjonsbeloepPrMnd(of.createEndringIRefusjonRefusjonsbeloepPrMnd(rp.getRefusjonPrMnd()));
            return endring;
        }).forEach(liste::add);
        refusjon.setEndringIRefusjonListe(of.createRefusjonEndringIRefusjonListe(endringListe));
        return of.createSkjemainnholdRefusjon(refusjon);
    }

    private static JAXBElement<Arbeidsforhold> lagArbeidsforholdXml(InntektsmeldingEntitet inntektsmeldingEntitet) {
        var arbeidsforhold = new Arbeidsforhold();

        // Inntekt
        var inntektBeløp = of.createInntektBeloep(inntektsmeldingEntitet.getMånedInntekt());
        var inntekt = new Inntekt();
        inntekt.setBeloep(inntektBeløp);
        // TODO Endringsarsak kan være enten "Tariffendring" eller "FeilInntekt", skal vi bruke disse?
        var inntektSkjemaVerdi = of.createArbeidsforholdBeregnetInntekt(inntekt);
        arbeidsforhold.setBeregnetInntekt(inntektSkjemaVerdi);

        // Startdato
        arbeidsforhold.setFoersteFravaersdag(of.createArbeidsforholdFoersteFravaersdag(inntektsmeldingEntitet.getStartDato()));
        return of.createSkjemainnholdArbeidsforhold(arbeidsforhold);
    }

    private static Kontaktinformasjon lagKontaktperson(InntektsmeldingEntitet inntektsmelding) {
        var ki = new Kontaktinformasjon();
        // Ved overstyring av inntektsmelding setter vi saksbehandlers informasjon her
        if (Kildesystem.FPSAK.equals(inntektsmelding.getKildesystem())) {
            ki.setTelefonnummer(inntektsmelding.getOpprettetAv());
            ki.setKontaktinformasjonNavn(inntektsmelding.getOpprettetAv());
        } else {
            var kontaktPerson = inntektsmelding.getKontaktperson();
            ki.setTelefonnummer(kontaktPerson.getTelefonnummer());
            ki.setKontaktinformasjonNavn(kontaktPerson.getNavn());
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
        NAV_NO
    }
}
