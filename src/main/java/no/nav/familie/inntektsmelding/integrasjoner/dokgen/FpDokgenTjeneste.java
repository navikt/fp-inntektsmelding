package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.integrasjoner.person.AktørId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.v1.FpDokgenRestKlient;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerValidator;

@ApplicationScoped
public class FpDokgenTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FpDokgenTjeneste.class);
    private FpDokgenRestKlient dokgenKlient;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;

    FpDokgenTjeneste() {
        //CDI
    }

    @Inject
    public FpDokgenTjeneste(FpDokgenRestKlient fpDokgenKlient,
                            PersonTjeneste personTjeneste,
                            OrganisasjonTjeneste organisasjonTjeneste) {
        this.dokgenKlient = fpDokgenKlient;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
    }

    public byte[] mapDataOgGenererPdf(InntektsmeldingEntitet inntektsmelding, ForespørselType forespørselType) {
        PersonInfo personInfo;
        String arbeidsgiverNavn;
        var arbeidsgvierIdent = inntektsmelding.getArbeidsgiverIdent();
        var inntektsmeldingsid = inntektsmelding.getId() != null ? inntektsmelding.getId().intValue() : 1;

        personInfo = personTjeneste.hentPersonInfoFraAktørId(new AktørId(inntektsmelding.getAktørId().getAktørId()), inntektsmelding.getYtelsetype());
        arbeidsgiverNavn = finnArbeidsgiverNavn(inntektsmelding, arbeidsgvierIdent);

        InntektsmeldingPdfData imDokumentData = InntektsmeldingPdfDataMapper.mapInntektsmeldingPdfData(inntektsmelding,
            arbeidsgiverNavn,
            personInfo,
            arbeidsgvierIdent,
            forespørselType);
        LOG.info("PDF av inntektsmelding med id {} ble generert.", inntektsmeldingsid);
        return dokgenKlient.genererPdf(imDokumentData, forespørselType);
    }

    private String finnArbeidsgiverNavn(InntektsmeldingEntitet inntektsmelding, String arbeidsgvierIdent) {
        String arbeidsgiverNavn;
        if (!OrganisasjonsnummerValidator.erGyldig(arbeidsgvierIdent)) {
            var personIdent = new PersonIdent(arbeidsgvierIdent);
            arbeidsgiverNavn = personTjeneste.hentPersonFraIdent(personIdent, inntektsmelding.getYtelsetype()).mapNavn();
        } else {
            arbeidsgiverNavn = organisasjonTjeneste.finnOrganisasjon(arbeidsgvierIdent).navn();
        }
        return arbeidsgiverNavn;
    }
}
