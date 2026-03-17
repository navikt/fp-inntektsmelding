package no.nav.foreldrepenger.inntektsmelding.imapi.rest.forespørsel;

import static no.nav.foreldrepenger.inntektsmelding.imapi.rest.forespørsel.ForespørselApiRest.BASE_PATH;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.foreldrepenger.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselStatusDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.YtelseTypeDto;

@AutentisertMedAzure
@ApplicationScoped
@Transactional
@Path(BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ForespørselApiRest {
    public static final String BASE_PATH = "/imapi/foresporsel";
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselApiRest.class);
    private ForespørselApiTjeneste forespørselApiTjeneste;
    private Tilgang tilgang;

    ForespørselApiRest() {
        // Kun for CDI-proxy
    }

    @Inject
    public ForespørselApiRest(ForespørselApiTjeneste forespørselApiTjeneste,
                              Tilgang tilgang) {
        this.forespørselApiTjeneste = forespørselApiTjeneste;
        this.tilgang = tilgang;
    }

    @GET
    @Path("/hent/{forespørselUuid}")
    @Tilgangskontrollert
    public Response hentForespørsel(@Valid @PathParam("forespørselUuid") UUID forespørselUuId) {
        sjekkErSystemkall();

        var forespørselDto = forespørselApiTjeneste.hentForesørselDto(forespørselUuId);

        if (forespørselDto.isEmpty()) {
            LOG.warn("Forespørsel med uuid {} finnes ikke", forespørselUuId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return forespørselDto.map(Response::ok).orElse(Response.status(Response.Status.NOT_FOUND)).build();
    }

    @POST
    @Path("/hent/foresporsler")
    @Tilgangskontrollert
    public Response hentForespørsler(@Valid @NotNull ForespørselFilterRequest filterRequest) {
        sjekkErSystemkall();
        var dtoer = forespørselApiTjeneste.hentForespørslerDto(filterRequest.orgnr(),
            filterRequest.fnr(),
            filterRequest.status(),
            filterRequest.ytelseType(),
            filterRequest.fom(),
            filterRequest.tom());
        return Response.ok(dtoer).build();
    }

    protected record ForespørselFilterRequest(@NotNull @Valid OrganisasjonsnummerDto orgnr,
                                              @Pattern(regexp = "^\\d{11}$") String fnr,
                                              @Valid ForespørselStatusDto status,
                                              @Valid YtelseTypeDto ytelseType,
                                              LocalDate fom,
                                              LocalDate tom) {}

    private void sjekkErSystemkall() {
        tilgang.sjekkErSystembruker();
    }
}
