package no.nav.familie.inntektsmelding.inntektsmelding;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.exceptions.FeilDto;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;

@AutentisertMedAzure
@RequestScoped
@Transactional
@Path(InntektsmeldingApiRest.BASE_PATH)
public class InntektsmeldingApiRest {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingApiRest.class);

    public static final String BASE_PATH = "/inntektsmelding";
    private Tilgang tilgangskontroll;
    private InntektsmeldingApiTjeneste inntektsmeldingApiTjeneste;

    InntektsmeldingApiRest() {
        // CDI
    }

    @Inject
    public InntektsmeldingApiRest(InntektsmeldingApiTjeneste inntektsmeldingApiTjeneste, Tilgang tilgangskontroll) {
        this.inntektsmeldingApiTjeneste = inntektsmeldingApiTjeneste;
        this.tilgangskontroll = tilgangskontroll;
    }

    @GET
    @Path("/hent")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter en tidligere innsendt inntektsmelding", tags = "ekstern-api")
    @Tilgangskontrollert
    public Response sendInntektsmelding(@Parameter(description = "Inntektsmelding unik UUID") @Valid @NotNull @QueryParam("inntektsmeldingUuid")
                                        UUID inntektsmeldingUuid) {
        tilgangskontroll.sjekkErSystembruker();
        LOG.trace("Henter inntektsmelding med UUID: {}", inntektsmeldingUuid);
        var inntektsmelding = inntektsmeldingApiTjeneste.hentInntektsmelding(inntektsmeldingUuid);

        if (inntektsmelding == null) {
            LOG.info("Fant ingen inntektsmelding for UUID: {}", inntektsmeldingUuid);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new FeilDto("Inntektsmelding med %s id finnes ikke.".formatted(inntektsmeldingUuid)))
                .type(MediaType.APPLICATION_JSON)
                .build();
        }

        return Response.ok(inntektsmelding).build();
    }
}
