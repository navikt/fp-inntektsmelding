package no.nav.foreldrepenger.inntektsmelding.imapi.rest.inntekt;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.foreldrepenger.inntektsmelding.server.tilgangsstyring.Tilgang;

@AutentisertMedAzure
@ApplicationScoped
@Path(InntektApiRest.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class InntektApiRest {
    public static final String BASE_PATH = "/imapi/inntekt";
    private static final Logger LOG = LoggerFactory.getLogger(InntektApiRest.class);
    private InntektApiTjeneste inntektApiTjeneste;
    private Tilgang tilgang;

    InntektApiRest() {
        // Kun for CDI-proxy
    }

    @Inject
    public InntektApiRest(InntektApiTjeneste inntektApiTjeneste, Tilgang tilgang) {
        this.inntektApiTjeneste = inntektApiTjeneste;
        this.tilgang = tilgang;
    }

    @GET
    @Path("/{forespørselUuid}")
    @Tilgangskontrollert
    public Response hentInntekt(@Valid @PathParam("forespørselUuid") UUID forespørselUuid) {
        tilgang.sjekkErSystembruker();

        var inntektDto = inntektApiTjeneste.hentInntektDto(forespørselUuid);

        if (inntektDto.isEmpty()) {
            LOG.warn("Forespørsel med uuid {} finnes ikke", forespørselUuid);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(inntektDto.get()).build();
    }
}
