package no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen.v1.FpDokgenRequest;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen.v1.FpDokgenRestKlient;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.utils.OrganisasjonsnummerValidator;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
public class DokumentGeneratorTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(DokumentGeneratorTjeneste.class);

    private FpDokgenRestKlient dokgenKlient;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;

    DokumentGeneratorTjeneste() {
        //CDI
    }

    @Inject
    public DokumentGeneratorTjeneste(FpDokgenRestKlient fpDokgenKlient,
                                     PersonTjeneste personTjeneste,
                                     OrganisasjonTjeneste organisasjonTjeneste) {
        this.dokgenKlient = fpDokgenKlient;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
    }

    public byte[] mapDataOgGenererPdf(InntektsmeldingDto inntektsmelding, ForespørselType forespørselType) {
        PersonInfo personInfo;
        String arbeidsgiverNavn;
        var arbeidsgvierIdent = inntektsmelding.getArbeidsgiver();
        var inntektsmeldingUuid = Optional.ofNullable(inntektsmelding.getInntektsmeldingUuid()).orElse(UUID.randomUUID());

        personInfo = personTjeneste.hentPersonInfoFraAktørId(inntektsmelding.getAktørId(), Ytelsetype.valueOf(inntektsmelding.getYtelse().name()));
        arbeidsgiverNavn = finnArbeidsgiverNavn(arbeidsgvierIdent, Ytelsetype.valueOf(inntektsmelding.getYtelse().name()));

        var imDokumentData = InntektsmeldingPdfDataMapper.mapInntektsmeldingPdfData(inntektsmelding,
            arbeidsgiverNavn,
            personInfo,
            arbeidsgvierIdent,
            forespørselType);
        LOG.info("PDF av inntektsmelding med uuid: {} ble generert.", inntektsmeldingUuid);
        return genererPdf(imDokumentData, forespørselType);
    }

    private String finnArbeidsgiverNavn(Arbeidsgiver arbeidsgvierIdent, Ytelsetype ytelsetype) {
        String arbeidsgiverNavn;
        if (!OrganisasjonsnummerValidator.erGyldig(arbeidsgvierIdent.orgnr())) {
            var personIdent = new PersonIdent(arbeidsgvierIdent.orgnr());
            arbeidsgiverNavn = personTjeneste.hentPersonFraIdent(personIdent, ytelsetype).mapNavn();
        } else {
            arbeidsgiverNavn = organisasjonTjeneste.finnOrganisasjon(arbeidsgvierIdent).navn();
        }
        return arbeidsgiverNavn;
    }

    private byte[] genererPdf(InntektsmeldingPdfData metadata, ForespørselType forespørselType) {
        var template = utledMal(forespørselType);
        var requestDto = new FpDokgenRequest(template, null, FpDokgenRequest.CssStyling.INNTEKTSMELDING_PDF,
            DefaultJsonMapper.toJson(metadata));

        return dokgenKlient.genererPdf(requestDto);
    }

    private String utledMal(ForespørselType forespørselType) {
        if (forespørselType == ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT) {
            return "fpinntektsmelding-refusjonskrav";
        }
        return "fpinntektsmelding-inntektsmelding";
    }
}
