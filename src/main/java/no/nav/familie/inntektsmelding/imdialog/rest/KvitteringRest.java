package no.nav.familie.inntektsmelding.imdialog.rest;

import java.util.Comparator;
import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;

@AutentisertMedTokenX
@RequestScoped
@Transactional
@Consumes(MediaType.APPLICATION_JSON)
@Path(KvitteringRest.BASE_PATH)
public class KvitteringRest {
    private static final Logger LOG = LoggerFactory.getLogger(KvitteringRest.class);
    private Tilgang tilgang;
    public static final String BASE_PATH = "/ekstern";
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    KvitteringRest() {
        // CDI
    }
    @Inject
    public KvitteringRest(Tilgang tilgang, InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.tilgang = tilgang;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    @GET
    @Path("/kvittering/{uuid}")
    @Produces("application/pdf")
    @Tilgangskontrollert
    public Response hentKvittering(@NotNull @Valid @PathParam("uuid") UUID forespørselUuid) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid);

        LOG.info("Henter inntektsmelding for forespørsel uuid {}", forespørselUuid);
        var inntektsmelding = inntektsmeldingTjeneste.hentInntektsmeldinger(forespørselUuid).stream()
            .max(Comparator.comparing(InntektsmeldingEntitet::getOpprettetTidspunkt))
            .orElse(null);

        if (inntektsmelding == null) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        var pdf = inntektsmeldingTjeneste.hentPDF(inntektsmelding.getId());

        var responseBuilder = Response.ok(pdf);
        responseBuilder.type("application/pdf");
        responseBuilder.header("Content-Disposition", "attachment; filename=inntektsmelding.pdf");
        return responseBuilder.build();
    }
}
