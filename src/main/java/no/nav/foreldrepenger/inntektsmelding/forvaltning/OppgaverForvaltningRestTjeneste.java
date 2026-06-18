package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.foreldrepenger.inntektsmelding.server.tilgangsstyring.Tilgang;

@AutentisertMedAzure
@OpenAPIDefinition(tags = @Tag(name = "oppgaver", description = "Håndtering av feilopprettede saker / oppgaver i arbeidsgiverportalen"))
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Path("/forvaltningOppgaver")
public class OppgaverForvaltningRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(OppgaverForvaltningRestTjeneste.class);

    private Tilgang tilgang;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    OppgaverForvaltningRestTjeneste() {
        // REST CDI
    }

    @Inject
    public OppgaverForvaltningRestTjeneste(Tilgang tilgang, ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.tilgang = tilgang;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    @POST
    @Path("/settTilUtgatt/{forespoerselUuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Setter angitt forespørsel og tilhørende sak i arbeidsgiverportalen til utgått", tags = "oppgaver", responses = {
        @ApiResponse(responseCode = "202", description = "Forespørsel og oppgave er satt til utgått", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response settForespørselOgSakTilUtgått(
        @Parameter(description = "UUID for forespørsel som skal settes til utgått") @Valid @NotNull @PathParam("forespoerselUuid")
        @Pattern(regexp = "^[a-fA-F\\d]{8}(?:-[a-fA-F\\d]{4}){3}-[a-fA-F\\d]{12}$", message = "Ugyldig UUID-format")
        String forespørselUuid) {
        var gyldigForespørselUuid = UUID.fromString(forespørselUuid);
        sjekkAtKallerHarRollenDrift();
        LOG.info("Setter forespørsel og tilhørende sak i arbeidsgiverportalen med forespørselUuid {} til utgått", forespørselUuid);
        forespørselBehandlingTjeneste.settForespørselTilUtgåttForvaltning(gyldigForespørselUuid);
        return Response.status(Response.Status.ACCEPTED).build();
    }

    private void sjekkAtKallerHarRollenDrift() {
        tilgang.sjekkAtAnsattHarRollenDrift();
    }
}
