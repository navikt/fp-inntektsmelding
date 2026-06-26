package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.foreldrepenger.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.SaksnummerDto;

@AutentisertMedAzure
@OpenAPIDefinition(tags = @Tag(name = "forespoersler", description = "Forvaltningshåndtering av forespørsler"))
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Path("/forvaltningForespoersler")
public class ForespørselForvaltningRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ForespørselForvaltningRestTjeneste.class);

    private Tilgang tilgang;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private ForespørselTjeneste forespørselTjeneste;
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;

    ForespørselForvaltningRestTjeneste() {
        // REST CDI
    }

    @Inject
    public ForespørselForvaltningRestTjeneste(Tilgang tilgang,
                                              ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                              ForespørselTjeneste forespørselTjeneste,
                                              MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste) {
        this.tilgang = tilgang;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
    }

    @POST
    @Path("/settTilUtgatt/{forespoerselUuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Setter angitt forespørsel og tilhørende sak i arbeidsgiverportalen til utgått", tags = "forespoersler", responses = {
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

    @POST
    @Path("/settOppgaveUtfoert/{forespoerselUuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Setter fageroppgaven knyttet til forespørsel til utført. Må unntaksvis brukes hvios fager oppgaver ikke er lukket som forventet", tags = "forespoersler", responses = {
        @ApiResponse(responseCode = "202", description = "Oppgave er satt til utført", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response settOppgavePåForespørselTilLøst(
        @Parameter(description = "UUID for forespørsel som skal settes til utgått") @Valid @NotNull @PathParam("forespoerselUuid")
        @Pattern(regexp = "^[a-fA-F\\d]{8}(?:-[a-fA-F\\d]{4}){3}-[a-fA-F\\d]{12}$", message = "Ugyldig UUID-format")
        String forespørselUuid) {
        var gyldigForespørselUuid = UUID.fromString(forespørselUuid);
        sjekkAtKallerHarRollenDrift();
        LOG.info("Setter forespørsel og tilhørende sak i arbeidsgiverportalen med forespørselUuid {} til utgått", forespørselUuid);
        var forespørsel = forespørselTjeneste.hentForespørsel(gyldigForespørselUuid).orElseThrow();
        minSideArbeidsgiverTjeneste.oppgaveUtført(forespørsel.oppgaveId(), OffsetDateTime.now());
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @GET
    @Path("/hentForSak/{saksnummer}")
    @Operation(description = "Henter alle forespørsler for et saksnummer med status og distribusjonsinformasjon", tags = "forespoersler", responses = {
        @ApiResponse(responseCode = "200", description = "Liste over forespørsler for saksnummer"),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response hentForespørslerForSaksnummer(
        @Parameter(description = "Saksnummer det skal hentes forespørsler for")
        @Valid @NotNull @PathParam("saksnummer") SaksnummerDto saksnummer) {
        sjekkAtKallerHarRollenDrift();
        LOG.info("Henter forespørsler for saksnummer {}", saksnummer);
        var forespørsler = forespørselTjeneste.finnForespørslerForFagsak(Saksnummer.fra(saksnummer.saksnr()));
        var response = forespørsler.stream()
            .map(f -> new ForvaltningForespørselDto(
                f.uuid(),
                f.arbeidsgiver().orgnr(),
                f.førsteUttaksdato(),
                f.skjæringstidspunkt(),
                f.status(),
                f.dialogportenUuid() != null,
                f.arbeidsgiverNotifikasjonSakId() != null))
            .toList();
        return Response.ok(response).build();
    }

    public record ForvaltningForespørselDto(UUID uuid,
                                            String organisasjonsnummer,
                                            LocalDate førsteUttaksdato,
                                            LocalDate skjæringstidspunkt,
                                            ForespørselStatus status,
                                            boolean sendtTilDialogporten,
                                            boolean sendtTilArbeidsgiverportalen) {
    }

    private void sjekkAtKallerHarRollenDrift() {
        tilgang.sjekkAtAnsattHarRollenDrift();
    }
}
