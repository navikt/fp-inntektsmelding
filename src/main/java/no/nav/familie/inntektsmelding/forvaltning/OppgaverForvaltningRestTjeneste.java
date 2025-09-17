package no.nav.familie.inntektsmelding.forvaltning;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.familie.inntektsmelding.forespørsel.rest.NyBeskjedRequest;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.konfig.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@AutentisertMedAzure
@OpenAPIDefinition(tags = @Tag(name = "oppgaver", description = "Hånstering av feilopprettede saker / oppgaver i arbeidsgiverportalen"))
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
    @Path("/slettOppgave")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Sletter en oppgave i arbeidsgiverportalen", summary = "Sletter en oppgave i arbeidsgiverportalen.", tags = "oppgaver", responses = {
        @ApiResponse(responseCode = "202", description = "Oppgaven er slettet", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response slettOppgave(
        @Parameter(description = "Informasjon om oppgaven") @Valid SlettOppgaveRequest inputDto) {
        sjekkAtKallerHarRollenDrift();
        LOG.info("Sletter oppgave med saksnummer {}", inputDto.saksnummer());
        forespørselBehandlingTjeneste.slettForespørsel(inputDto.saksnummer(), inputDto.orgnr(), inputDto.stp());
        return Response.ok().build();
    }

    @POST
    @Path("/nyBeskjed")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter ny beskjed i arbeidsgiverportalen", summary = "Oppretter ny beskjed i arbeidsgiverportalen.", tags = "oppgaver", responses = {
        @ApiResponse(responseCode = "202", description = "Beskjeden er sendt", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response nyBeskjed(
        @Parameter(description = "Informasjon om oppgaven") @Valid NyBeskjedRequest inputDto) {
        sjekkAtKallerHarRollenDrift();
        if (Environment.current().isProd()) {
            // Kun ment for test i dev
            return Response.ok().build();
        }
        LOG.info("Oppretter beskjed på oppgave med saksnummer {}", inputDto.fagsakSaksnummer());
        var resultat = forespørselBehandlingTjeneste.opprettNyBeskjedMedEksternVarsling(inputDto.fagsakSaksnummer(), inputDto.orgnummer());
        LOG.info("Resultat for opprett beskjed {}", resultat.name());
        return Response.ok(resultat).build();
    }

    public record SlettOppgaveRequest(@Valid @NotNull SaksnummerDto saksnummer, @Valid @NotNull OrganisasjonsnummerDto orgnr,
                                         @Valid LocalDate stp) {}

    private void sjekkAtKallerHarRollenDrift() {
        tilgang.sjekkAtAnsattHarRollenDrift();
    }
}
