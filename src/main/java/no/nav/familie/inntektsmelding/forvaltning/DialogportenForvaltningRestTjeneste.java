package no.nav.familie.inntektsmelding.forvaltning;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.altinn.DialogportenKlient;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.konfig.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@ApplicationScoped
@Path(DialogportenForvaltningRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@AutentisertMedAzure
public class DialogportenForvaltningRestTjeneste {
    public static final String BASE_PATH = "/dialogporten";
    private static final Logger LOG = LoggerFactory.getLogger(DialogportenForvaltningRestTjeneste.class);
    private static final boolean IS_PROD = Environment.current().isProd();
    private Tilgang tilgang;
    private DialogportenKlient dialogportenKlient;

    DialogportenForvaltningRestTjeneste() {
        // REST CDI
    }

    @Inject
    public DialogportenForvaltningRestTjeneste(Tilgang tilgang, DialogportenKlient dialogportenKlient) {
        this.tilgang = tilgang;
        this.dialogportenKlient = dialogportenKlient;
    }

    @POST
    @Path("/opprettDialog")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Ny Dialog i Dialogporten med en Inntektsmelding transmission", tags = "dialogporten")
    @Tilgangskontrollert
    public Response opprettDialog(@NotNull @Valid OpprettNyDialogDto opprettNyDialogDto) {
        if (IS_PROD) {
            throw new IllegalStateException("Kan ikke opprette dialog i produksjon. Bruk testmiljø for dette.");
        }
        sjekkAtKallerHarRollenDrift();
        LOG.info("Oppretter en dialog for foresprørselUuid {} og organisasjonsnummer {}", opprettNyDialogDto.forespørselUuid(), opprettNyDialogDto.organisasjonsnummer().orgnr());
        return Response.accepted(dialogportenKlient.opprettDialog(opprettNyDialogDto.forespørselUuid(), opprettNyDialogDto.organisasjonsnummer(), "Foresprøsel om inntektsmelding")).build();
    }

    private void sjekkAtKallerHarRollenDrift() {
        tilgang.sjekkAtAnsattHarRollenDrift();
    }

    public record OpprettNyDialogDto(@NotNull @Valid UUID forespørselUuid, @NotNull @Valid OrganisasjonsnummerDto organisasjonsnummer) {
    }
}
