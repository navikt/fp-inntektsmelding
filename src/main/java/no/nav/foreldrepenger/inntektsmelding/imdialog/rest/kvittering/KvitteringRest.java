package no.nav.foreldrepenger.inntektsmelding.imdialog.rest.kvittering;

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

import no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester.KvitteringTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.foreldrepenger.inntektsmelding.server.tilgangsstyring.Tilgang;

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
    private KvitteringTjeneste kvitteringTjeneste;

    KvitteringRest() {
        // CDI
    }
    @Inject
    public KvitteringRest(Tilgang tilgang, InntektsmeldingTjeneste inntektsmeldingTjeneste, KvitteringTjeneste kvitteringTjeneste) {
        this.tilgang = tilgang;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.kvitteringTjeneste = kvitteringTjeneste;
    }

    /**
     * Deprekert. Bruk heller /innsendt/{uuid} endepunkt
     * @param forespørselUuid
     * @return
     */
    @Deprecated
    @GET
    @Path("/kvittering/{uuid}")
    @Produces("application/pdf")
    @Tilgangskontrollert
    public Response hentKvitteringForespørsel(@NotNull @Valid @PathParam("uuid") UUID forespørselUuid) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid);

        LOG.info("Henter inntektsmelding for forespørsel uuid {}", forespørselUuid);
        var inntektsmelding = inntektsmeldingTjeneste.hentInntektsmeldinger(forespørselUuid).stream()
            .max(Comparator.comparing(InntektsmeldingDto::getInnsendtTidspunkt))
            .orElse(null);

        if (inntektsmelding == null) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        var pdf = kvitteringTjeneste.hentPDF(inntektsmelding.getId());

        var responseBuilder = Response.ok(pdf);
        responseBuilder.type("application/pdf");
        responseBuilder.header("Content-Disposition", "attachment; filename=inntektsmelding.pdf");
        return responseBuilder.build();
    }

    /**
     * Deprekert. Bruk heller /innsendt/inntektsmelding/{uuid} endepunkt
     * @param inntektsmeldingUuid
     * @return
     */
    @Deprecated
    @GET
    @Path("/kvittering/inntektsmelding/{uuid}")
    @Produces("application/pdf")
    @Tilgangskontrollert
    public Response hentKvitteringInntektsmelding(@NotNull @Valid @PathParam("uuid") UUID inntektsmeldingUuid) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilInntektsmelding(inntektsmeldingUuid);
        var im = inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingUuid);
        if (im == null) {
            LOG.info("Finner ikke inntektsmelding med uuid {}", inntektsmeldingUuid);
            return Response.status(Response.Status.NO_CONTENT).build();
        } else {
            var pdf = kvitteringTjeneste.hentPDF(im.getId());
            var ytelsetekst = im.getYtelse().equals(Ytelsetype.FORELDREPENGER) ? "foreldrepenger" : "svangerskapspenger";
            var siste12TegnFraUuid = inntektsmeldingUuid.toString().substring(inntektsmeldingUuid.toString().length() - 12);
            var responseBuilder = Response.ok(pdf);
            responseBuilder.type("application/pdf");
            responseBuilder.header("Content-Disposition", String.format("attachment; filename=inntektsmelding-%s-%s.pdf", ytelsetekst, siste12TegnFraUuid));
            LOG.info("Returnerer pdf for inntektsmelding med id {}", inntektsmeldingUuid);
            return responseBuilder.build();
        }
    }

    @GET
    @Path("/innsendt/{uuid}")
    @Produces("application/pdf")
    @Tilgangskontrollert
    public Response hentForInnsendtInntektsmeldingPåForespørsel(@NotNull @Valid @PathParam("uuid") UUID forespørselUuid) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid);

        LOG.info("Henter inntektsmelding for forespørsel uuid {}", forespørselUuid);
        var inntektsmelding = inntektsmeldingTjeneste.hentInntektsmeldinger(forespørselUuid).stream()
            .max(Comparator.comparing(InntektsmeldingDto::getInnsendtTidspunkt))
            .orElse(null);

        if (inntektsmelding == null) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        var pdf = kvitteringTjeneste.hentPDF(inntektsmelding.getId());

        var responseBuilder = Response.ok(pdf);
        responseBuilder.type("application/pdf");
        responseBuilder.header("Content-Disposition", "attachment; filename=inntektsmelding.pdf");
        return responseBuilder.build();
    }

    @GET
    @Path("/innsendt/inntektsmelding/{uuid}")
    @Produces("application/pdf")
    @Tilgangskontrollert
    public Response hentInnsendtInntektsmelding(@NotNull @Valid @PathParam("uuid") UUID inntektsmeldingUuid) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilInntektsmelding(inntektsmeldingUuid);
        var im = inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingUuid);
        if (im == null) {
            LOG.info("Finner ikke inntektsmelding med uuid {}", inntektsmeldingUuid);
            return Response.status(Response.Status.NO_CONTENT).build();
        } else {
            var pdf = kvitteringTjeneste.hentPDF(im.getId());
            var ytelsetekst = im.getYtelse().equals(Ytelsetype.FORELDREPENGER) ? "foreldrepenger" : "svangerskapspenger";
            var siste12TegnFraUuid = inntektsmeldingUuid.toString().substring(inntektsmeldingUuid.toString().length() - 12);
            var responseBuilder = Response.ok(pdf);
            responseBuilder.type("application/pdf");
            responseBuilder.header("Content-Disposition", String.format("attachment; filename=inntektsmelding-%s-%s.pdf", ytelsetekst, siste12TegnFraUuid));
            LOG.info("Returnerer pdf for inntektsmelding med id {}", inntektsmeldingUuid);
            return responseBuilder.build();
        }
    }
}
