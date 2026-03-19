package no.nav.familie.inntektsmelding.imdialog.rest.ekstern;

import static no.nav.familie.inntektsmelding.forespørsel.rest.ekstern.ForespørselEksternRest.BASE_PATH;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.tjenester.ekstern.EksternInntektsmeldingMottakTjeneste;
import no.nav.familie.inntektsmelding.imdialog.tjenester.ekstern.EksternInntektsmeldingResponseDto;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;

@AutentisertMedAzure
@ApplicationScoped
@Transactional
@Path(BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InntektsmeldingEksternRest {
    public static final String BASE_PATH = "/inntektsmelding-ekstern";
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingEksternRest.class);
    private Tilgang tilgang;
    private EksternInntektsmeldingMottakTjeneste eksternInntektsmeldingMottakTjeneste;

    InntektsmeldingEksternRest() {
        // Kun for CDI-proxy
    }

    @Inject
    public InntektsmeldingEksternRest(Tilgang tilgang,
                                      EksternInntektsmeldingMottakTjeneste eksternInntektsmeldingMottakTjeneste) {
        this.tilgang = tilgang;
        this.eksternInntektsmeldingMottakTjeneste = eksternInntektsmeldingMottakTjeneste;
    }


    @POST
    @Path("/send-inntektsmelding")
    @Tilgangskontrollert
    public EksternInntektsmeldingResponseDto sendEksternInntektsmelding(SendInntektsmeldingEksternRequest sendInntektsmeldingEksternRequest) {
        sjekkErSystemkall();
        LOG.info("Mottatt inntektsmelding fra ekstern kilde for forespørselUuid {} ", sendInntektsmeldingEksternRequest.foresporselUuid());
        return eksternInntektsmeldingMottakTjeneste.mottaEksternInntektsmelding(sendInntektsmeldingEksternRequest);
    }

    private void sjekkErSystemkall() {
        tilgang.sjekkErSystembruker();
    }
}
