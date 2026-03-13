package no.nav.familie.inntektsmelding.imdialog.task;

import java.io.StringWriter;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerValidator;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.seres.xsd.nav.inntektsmelding_m._20181211.InntektsmeldingM;
import no.seres.xsd.nav.inntektsmelding_m._20181211.ObjectFactory;

@ApplicationScoped
public class InntektsmeldingXMLTjeneste {
    private PersonTjeneste personTjeneste;

    InntektsmeldingXMLTjeneste() {
        // CDI
    }

    @Inject
    public InntektsmeldingXMLTjeneste(PersonTjeneste personTjeneste) {
        this.personTjeneste = personTjeneste;
    }

    public String lagXMLAvInntektsmelding(InntektsmeldingEntitet inntektsmelding) {
        var søkerIdent = personTjeneste.finnPersonIdentForAktørId(new AktørId(inntektsmelding.getAktørId().getAktørId()));
        var aktørIdIdentMap = Map.of(inntektsmelding.getAktørId(), søkerIdent);
        if (!OrganisasjonsnummerValidator.erGyldig(inntektsmelding.getArbeidsgiverIdent()) && inntektsmelding.getArbeidsgiverIdent().length() == 13) {
            var arbeidsgiverAktørId = new AktørIdEntitet(inntektsmelding.getArbeidsgiverIdent());
            var arbeidsgiverIdent = personTjeneste.finnPersonIdentForAktørId(new AktørId(arbeidsgiverAktørId.getAktørId()));
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
