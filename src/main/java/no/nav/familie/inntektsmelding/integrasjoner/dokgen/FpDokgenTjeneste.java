package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.v1.NyFpDokgenRestKlient;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerValidator;
import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
public class FpDokgenTjeneste {
    protected static final Environment ENV = Environment.current();
    private static final Logger LOG = LoggerFactory.getLogger(FpDokgenTjeneste.class);
    private NyFpDokgenRestKlient nyDokgenKlient;
    private FpDokgenKlient gammelDokgenKlient;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;

    FpDokgenTjeneste() {
        //CDI
    }

    @Inject
    public FpDokgenTjeneste(NyFpDokgenRestKlient fpDokgenKlient,
                            FpDokgenKlient gammelDokgenKlient,
                            PersonTjeneste personTjeneste,
                            OrganisasjonTjeneste organisasjonTjeneste) {
        this.nyDokgenKlient = fpDokgenKlient;
        this.gammelDokgenKlient = gammelDokgenKlient;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
    }

    public byte[] mapDataOgGenererPdf(InntektsmeldingEntitet inntektsmelding, ForespørselType forespørselType) {
        PersonInfo personInfo;
        String arbeidsgiverNavn;
        var arbeidsgvierIdent = inntektsmelding.getArbeidsgiverIdent();
        var inntektsmeldingsid = inntektsmelding.getId() != null ? inntektsmelding.getId().intValue() : 1;

        personInfo = personTjeneste.hentPersonInfoFraAktørId(inntektsmelding.getAktørId(), inntektsmelding.getYtelsetype());
        arbeidsgiverNavn = finnArbeidsgiverNavn(inntektsmelding, arbeidsgvierIdent);

        return genererPdf(InntektsmeldingPdfDataMapper.mapInntektsmeldingPdfData(inntektsmelding,
            arbeidsgiverNavn,
            personInfo,
            arbeidsgvierIdent,
            forespørselType), inntektsmeldingsid, forespørselType);
    }

    private byte[] genererPdf(InntektsmeldingPdfData imDokumentData, int inntektsmeldingId, ForespørselType forespørselType) {
        byte[] pdf;
        try {
            LOG.info("Genererer PDF ved bruk av ny dokgen.");
            pdf = nyDokgenKlient.genererPdf(imDokumentData, forespørselType);
        } catch (Exception exception) {
            if (ENV.isDev() || ENV.isProd()) {
                LOG.warn("Kall til ny dokgen feilet, prøver å generere PDF med gammel dokgen. Feilmelding: {}", exception.getMessage());
                pdf = gammelDokgenKlient.genererPdf(imDokumentData, forespørselType);
            } else {
                throw exception;
            }
        }
        LOG.info("Pdf av inntektsmelding med id {} ble generert.", inntektsmeldingId);
        return pdf;
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
