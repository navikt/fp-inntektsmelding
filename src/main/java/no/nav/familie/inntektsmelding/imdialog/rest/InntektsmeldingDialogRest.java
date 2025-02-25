package no.nav.familie.inntektsmelding.imdialog.rest;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.familie.inntektsmelding.imdialog.tjenester.GrunnlagDtoTjeneste;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingMapper;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingMottakTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;

@AutentisertMedTokenX
@RequestScoped
@Transactional
@Consumes(MediaType.APPLICATION_JSON)
@Path(InntektsmeldingDialogRest.BASE_PATH)
public class InntektsmeldingDialogRest {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingDialogRest.class);

    public static final String BASE_PATH = "/imdialog";
    private static final String HENT_OPPLYSNINGER = "/opplysninger";
    private static final String HENT_INNTEKTSMELDINGER_FOR_OPPGAVE = "/inntektsmeldinger";
    private static final String SEND_INNTEKTSMELDING = "/send-inntektsmelding";
    private static final String LAST_NED_PDF = "/last-ned-pdf";

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private GrunnlagDtoTjeneste grunnlagDtoTjeneste;
    private InntektsmeldingMottakTjeneste inntektsmeldingMottakTjeneste;
    private Tilgang tilgang;

    InntektsmeldingDialogRest() {
        // CDI
    }

    @Inject
    public InntektsmeldingDialogRest(InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                     GrunnlagDtoTjeneste grunnlagDtoTjeneste,
                                     InntektsmeldingMottakTjeneste inntektsmeldingMottakTjeneste,
                                     Tilgang tilgang) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.grunnlagDtoTjeneste = grunnlagDtoTjeneste;
        this.inntektsmeldingMottakTjeneste = inntektsmeldingMottakTjeneste;
        this.tilgang = tilgang;
    }

    @GET
    @Path(HENT_OPPLYSNINGER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentOpplysninger(@Valid @NotNull @QueryParam("foresporselUuid") UUID forespørselUuid) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid);

        LOG.info("Henter forespørsel med uuid {}", forespørselUuid);
        var dto = grunnlagDtoTjeneste.lagDialogDto(forespørselUuid);
        return Response.ok(dto).build();

    }

    @GET
    @Path(HENT_INNTEKTSMELDINGER_FOR_OPPGAVE)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentInntektsmeldingerForOppgave(@NotNull @Valid @QueryParam("foresporselUuid") UUID forespørselUuid) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid);

        LOG.info("Henter inntektsmeldinger for forespørsel {}", forespørselUuid);
        var dto = inntektsmeldingTjeneste.hentInntektsmeldinger(forespørselUuid).stream()
            .map(im -> InntektsmeldingMapper.mapFraEntitet(im, forespørselUuid))
            .toList();
        return Response.ok(dto).build();
    }

    @POST
    @Path(SEND_INNTEKTSMELDING)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response sendInntektsmelding(@NotNull @Valid SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {

        if (sendInntektsmeldingRequestDto.foresporselUuid() == null) {
            tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(new OrganisasjonsnummerDto(sendInntektsmeldingRequestDto.arbeidsgiverIdent().ident()));
            LOG.info("Mottok arbeisgiverinitert inntektsmelding for aktørId {}", sendInntektsmeldingRequestDto.aktorId());
           return Response.ok(inntektsmeldingMottakTjeneste.mottaArbeidsgiverInitiertInntektsmelding(sendInntektsmeldingRequestDto)).build();
        } else {
            tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(sendInntektsmeldingRequestDto.foresporselUuid());
            LOG.info("Mottok inntektsmelding for forespørsel {}", sendInntektsmeldingRequestDto.foresporselUuid());
            return Response.ok(inntektsmeldingMottakTjeneste.mottaInntektsmelding(sendInntektsmeldingRequestDto)).build();
        }
    }

    @GET
    @Path(LAST_NED_PDF)
    @Produces("application/pdf")
    @Tilgangskontrollert
    public Response lastNedPDF(@NotNull @Valid @QueryParam("id") long inntektsmeldingId) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(inntektsmeldingId);

        LOG.info("Henter inntektsmelding for id {}", inntektsmeldingId);
        var pdf = inntektsmeldingTjeneste.hentPDF(inntektsmeldingId);

        var responseBuilder = Response.ok(pdf);
        responseBuilder.type("application/pdf");
        responseBuilder.header("Content-Disposition", "attachment; filename=inntektsmelding.pdf");
        return responseBuilder.build();
    }

}
