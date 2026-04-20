package no.nav.foreldrepenger.inntektsmelding.imdialog.rest;

import static no.nav.foreldrepenger.inntektsmelding.typer.dto.ArbeidsgiverinitiertÅrsakDto.NYANSATT;
import static no.nav.foreldrepenger.inntektsmelding.typer.dto.ArbeidsgiverinitiertÅrsakDto.UREGISTRERT;
import static no.nav.foreldrepenger.inntektsmelding.typer.dto.KodeverkMapper.mapArbeidsgiverinitiertÅrsak;

import java.util.List;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester.GrunnlagDtoTjeneste;
import no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester.InntektsmeldingMapper;
import no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester.InntektsmeldingMottakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester.KvitteringTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.foreldrepenger.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;

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

    private KvitteringTjeneste kvitteringTjeneste;
    private GrunnlagDtoTjeneste grunnlagDtoTjeneste;
    private InntektsmeldingMottakTjeneste inntektsmeldingMottakTjeneste;
    private ForespørselBehandlingTjeneste forespørselTjeneste;
    private Tilgang tilgang;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    InntektsmeldingDialogRest() {
        // CDI
    }

    @Inject
    public InntektsmeldingDialogRest(KvitteringTjeneste kvitteringTjeneste,
                                     GrunnlagDtoTjeneste grunnlagDtoTjeneste,
                                     InntektsmeldingMottakTjeneste inntektsmeldingMottakTjeneste,
                                     ForespørselBehandlingTjeneste forespørselTjeneste,
                                     Tilgang tilgang, InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.kvitteringTjeneste = kvitteringTjeneste;
        this.grunnlagDtoTjeneste = grunnlagDtoTjeneste;
        this.inntektsmeldingMottakTjeneste = inntektsmeldingMottakTjeneste;
        this.forespørselTjeneste = forespørselTjeneste;
        this.tilgang = tilgang;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
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
        var forespørselEntitet = forespørselTjeneste.hentForespørsel(forespørselUuid).orElseThrow(() -> new IllegalStateException("Finner ingen forespørsel for id: " + forespørselUuid));
        LOG.info("Henter inntektsmeldinger for forespørsel {}", forespørselUuid);
        var dto = inntektsmeldingTjeneste.hentInntektsmeldinger(forespørselUuid).stream()
            .filter(im -> Kildesystem.ARBEIDSGIVERPORTAL.equals(im.getKildesystem())) // Vi skal ikke vise inntektsmeldinger som er overstyrt av driftstilganger / saksbehandlere
            .map(im -> InntektsmeldingMapper.mapFraDomene(im, forespørselEntitet))
            .toList();
        return Response.ok(dto).build();
    }

    @POST
    @Path(SEND_INNTEKTSMELDING)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response sendInntektsmelding(@NotNull @Valid SendInntektsmeldingRequestDto request) {
        var arbeidsgiverinitiertÅrsak = request.arbeidsgiverinitiertÅrsak();

        if (arbeidsgiverinitiertÅrsak != null && List.of(NYANSATT, UREGISTRERT).contains(arbeidsgiverinitiertÅrsak)) {
            tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(Arbeidsgiver.fra(request.arbeidsgiverIdent().orgnr()));

            var mottattInntektsmelding = NYANSATT.equals(arbeidsgiverinitiertÅrsak)
                                    ? InntektsmeldingMapper.mapTilDtoArbeidsgiverinitiert(request)
                                    : InntektsmeldingMapper.mapTilDto(request);

            LOG.info("Mottok arbeidsgiverinitert inntektsmelding årsak nyansatt for aktørId {}", request.aktorId());
           return Response.ok(inntektsmeldingMottakTjeneste.mottaArbeidsgiverinitiertInntektsmelding(mottattInntektsmelding, request.foresporselUuid(),
               mapArbeidsgiverinitiertÅrsak(arbeidsgiverinitiertÅrsak))).build();
        } else {
            tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(request.foresporselUuid());
            LOG.info("Mottok inntektsmelding for forespørsel {}", request.foresporselUuid());
            return Response.ok(inntektsmeldingMottakTjeneste.mottaInntektsmelding(InntektsmeldingMapper.mapTilDto(request), request.foresporselUuid())).build();
        }
    }

    @Deprecated
    @GET
    @Path(LAST_NED_PDF)
    @Produces("application/pdf")
    @Tilgangskontrollert
    public Response lastNedPDF(@NotNull @Valid @QueryParam("id") long inntektsmeldingId) {
        LOG.info("IM_PDF_DEPRECATED: Kall på deprekert endepunkt for å hente inntektsmeldingpdf");
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(inntektsmeldingId);
        var pdf = kvitteringTjeneste.hentPDF(inntektsmeldingId);
        var responseBuilder = Response.ok(pdf);
        responseBuilder.type("application/pdf");
        responseBuilder.header("Content-Disposition", "attachment; filename=inntektsmelding.pdf");
        return responseBuilder.build();
    }

}
