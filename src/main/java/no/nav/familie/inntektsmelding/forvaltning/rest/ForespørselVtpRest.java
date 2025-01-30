package no.nav.familie.inntektsmelding.forvaltning.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.foreldrepenger.konfig.Environment;

@AutentisertMedAzure
@OpenAPIDefinition(tags = @Tag(name = "vtp", description = "Rest endepunkter brukt for å gjøre testing enklere."))
@ApplicationScoped
@Transactional
@Path(ForespørselVtpRest.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ForespørselVtpRest {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselVtpRest.class);
    public static final String BASE_PATH = "/foresporsel";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    ForespørselVtpRest() {
        // Kun for CDI-proxy
    }

    @Inject
    public ForespørselVtpRest(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    // Dette endpointet brukes til verdikjedetesting, ikke i bruk i prod
    @GET
    @Path("/list/{saksnummer}")
    @Operation(description = "Leverer en liste med forespørsler opprettet for en sak.", summary = "Hent forespørsel for en sak", tags = "vtp")
    @Tilgangskontrollert
    public Response finnForespoerselForSaksnummer(
        @Parameter(description = "Saksnummer det skal listes ut forespørsler for")
        @Valid @NotNull @PathParam("saksnummer") SaksnummerDto saksnummer) {
        if (!Environment.current().isLocal()) {
            throw new RuntimeException("Endepunkt for listing av forespørsler per sak skal kun brukes for verdikjedetesting, lokalt eller på github");
        }
        LOG.info("Mottok forespørsel om uuid for forespørsel for sak {}", saksnummer);
        var forespørsler = forespørselBehandlingTjeneste.finnForespørslerForFagsak(saksnummer);
        return Response.ok(new ListForespørslerResponse(forespørsler)).build();
    }
}

