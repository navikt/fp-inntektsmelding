package no.nav.familie.inntektsmelding.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnDialogportenKlient;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
@Path(DialogportenForvaltningRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@AutentisertMedAzure
public class DialogportenForvaltningRestTjeneste {
    public static final String BASE_PATH = "/dialogporten";
    private static final Logger LOG = LoggerFactory.getLogger(DialogportenForvaltningRestTjeneste.class);
    private static final boolean IS_PROD = Environment.current().isProd();
    private Tilgang tilgang;
    private AltinnDialogportenKlient altinnDialogportenKlient;

    public DialogportenForvaltningRestTjeneste() {
        // REST CDI
    }
    @Inject
    public DialogportenForvaltningRestTjeneste(Tilgang tilgang, AltinnDialogportenKlient altinnDialogportenKlient) {
        this.tilgang = tilgang;
        this.altinnDialogportenKlient = altinnDialogportenKlient;
    }

    @POST
    @Path("/opprettDialog")
    @Consumes(MediaType.APPLICATION_JSON)
    @Tilgangskontrollert
    public String opprettDialog(String forespørselUuid, String organisasjonsnummer) {
        if (IS_PROD) {
            throw new IllegalStateException("Kan ikke opprette dialog i produksjon. Bruk testmiljø for dette.");
        }
        sjekkAtKallerHarRollenDrift();
        LOG.info("Oppretter en dialog for foresprørselUuid {} og organisasjonsnummer {}", forespørselUuid, organisasjonsnummer);
        return altinnDialogportenKlient.opprettDialog(forespørselUuid, organisasjonsnummer);
    }

    private void sjekkAtKallerHarRollenDrift() {
        tilgang.sjekkAtAnsattHarRollenDrift();
    }
}
