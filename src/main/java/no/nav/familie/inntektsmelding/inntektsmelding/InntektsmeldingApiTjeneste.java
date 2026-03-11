package no.nav.familie.inntektsmelding.inntektsmelding;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import java.util.UUID;
@Dependent
public class InntektsmeldingApiTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingApiTjeneste.class);
    private InntektsmeldingRepository inntektsmeldingRepository;
    InntektsmeldingApiTjeneste() {
        // CDI proxy
    }
    @Inject
    public InntektsmeldingApiTjeneste(InntektsmeldingRepository inntektsmeldingRepository) {
        this.inntektsmeldingRepository = inntektsmeldingRepository;
    }
    public InntektsmeldingEntitet hentInntektsmelding(UUID inntektsmeldingUuid) {
        return inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingUuid).orElse(null);
    }
}
