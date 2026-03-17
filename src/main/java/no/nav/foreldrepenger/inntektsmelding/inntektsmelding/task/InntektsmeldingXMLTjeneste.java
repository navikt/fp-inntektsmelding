package no.nav.foreldrepenger.inntektsmelding.inntektsmelding.task;

import java.io.StringWriter;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.utils.OrganisasjonsnummerValidator;
import no.seres.xsd.nav.inntektsmelding_m._20181211.InntektsmeldingM;
import no.seres.xsd.nav.inntektsmelding_m._20181211.ObjectFactory;

@ApplicationScoped
class InntektsmeldingXMLTjeneste {
    private PersonTjeneste personTjeneste;

    InntektsmeldingXMLTjeneste() {
        // CDI
    }

    @Inject
    public InntektsmeldingXMLTjeneste(PersonTjeneste personTjeneste) {
        this.personTjeneste = personTjeneste;
    }

    public String lagXMLAvInntektsmelding(InntektsmeldingDto inntektsmelding) {
        var aktørIdIdentMap = new HashMap<AktørId, PersonIdent>();

        var søkerAktørId = inntektsmelding.getAktørId();
        var søkerIdent = personTjeneste.finnPersonIdentForAktørId(søkerAktørId);
        aktørIdIdentMap.put(søkerAktørId, søkerIdent);

        var arbeidsgiver = inntektsmelding.getArbeidsgiver().orgnr();
        if (!OrganisasjonsnummerValidator.erGyldig(arbeidsgiver) && arbeidsgiver.length() == 13) {
            var arbeidsgiverAktørId = new AktørId(arbeidsgiver);
            var arbeidsgiverIdent = personTjeneste.finnPersonIdentForAktørId(arbeidsgiverAktørId);
            aktørIdIdentMap.put(arbeidsgiverAktørId, arbeidsgiverIdent);
        }
        var inntektsmeldingXml = InntektsmeldingXMLMapper.map(inntektsmelding, aktørIdIdentMap);
        try {
            return marshalXml(inntektsmeldingXml);
        } catch (JAXBException ex) {
            throw new IllegalStateException("Feil ved marshalling av XML " + ex);
        }
    }

    private String marshalXml(InntektsmeldingM imWrapper) throws JAXBException {
        var kontekst = JAXBContext.newInstance(ObjectFactory.class);
        var writer = new StringWriter();
        kontekst.createMarshaller().marshal(new ObjectFactory().createMelding(imWrapper), writer);
        return writer.toString();
    }

}
