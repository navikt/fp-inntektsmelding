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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import no.nav.foreldrepenger.inntektsmelding.imapi.inntekt.InntektResponse;

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
    @Operation(
        summary = "Henter inntekt fra A-ordningen for en forespørsel",
        description = """
            Henter inntekt rapportert til A-ordningen for arbeidstakeren i forespørselen, per måned og som gjennomsnitt.

            Verdier i `inntektPerMåned`:
            - Et tall (også `0`) betyr at arbeidsgiver har rapportert inntekt for måneden. `0` betyr at det er rapportert en inntekt på 0 kr.
            - `null` betyr at det ikke er rapportert inntekt for måneden, for eksempel fordi rapporteringsfristen ikke er passert eller
              arbeidstakeren er nyansatt. Måneden er likevel med i responsen.

            Hvis rapporteringsfristen ikke er passert for de nyeste månedene, kan responsen inneholde opptil fem måneder,
            slik at eldre rapporterte måneder kan brukes i gjennomsnittet.

            `gjennomsnitt` er gjennomsnittlig månedsinntekt for tre måneder. Måneder med `null` der rapporteringsfristen er passert,
            teller som 0 kr. For nyansatte regnes gjennomsnittet bare av månedene med rapportert inntekt, og er 0 hvis ingen er rapportert.

            Hvis inntekt ikke kan hentes fra A-ordningen (for eksempel ved nedetid), eller forespørselen ikke finnes, svarer endepunktet 404.
            """,
        tags = "ekstern-api",
        responses = {
            @ApiResponse(responseCode = "200", description = "Inntekt per måned og gjennomsnitt. `null` = ikke rapportert, `0` = rapportert inntekt på 0 kr.",
                content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = InntektResponse.class))),
            @ApiResponse(responseCode = "404", description = "Forespørselen finnes ikke, eller inntekt kunne ikke hentes fra A-ordningen.")
        })
    @Tilgangskontrollert
    public Response hentInntekt(@Valid @PathParam("forespørselUuid") UUID forespørselUuid) {
        tilgang.sjekkErSystembruker();

        var inntektDto = inntektApiTjeneste.hentInntektDto(forespørselUuid);

        if (inntektDto.isEmpty()) {
            LOG.info("Fant ikke inntekt for forespørsel med uuid {} (finnes ikke eller inntekt kunne ikke hentes)", forespørselUuid);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(inntektDto.get()).build();
    }
}
