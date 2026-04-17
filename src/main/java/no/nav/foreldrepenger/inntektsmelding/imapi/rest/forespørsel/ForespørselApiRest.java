package no.nav.foreldrepenger.inntektsmelding.imapi.rest.forespørsel;

import static no.nav.foreldrepenger.inntektsmelding.imapi.rest.forespørsel.ForespørselApiRest.BASE_PATH;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.inntektsmelding.felles.FødselsnummerDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.imapi.forespørsel.ForespørselFilterRequest;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.foreldrepenger.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Fødselsnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselStatusDto;
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
        var dtoer = forespørselApiTjeneste.hentForespørslerDto(
            Arbeidsgiver.fra(filterRequest.orgnr().orgnr()),
            Optional.ofNullable(filterRequest.fnr()).map(FødselsnummerDto::fnr).map(Fødselsnummer::new).orElse(null),
            Optional.ofNullable(filterRequest.status()).map(ForespørselApiRest::mapStatus).orElse(null),
            Optional.ofNullable(filterRequest.ytelseType()).map(ForespørselApiRest::mapYtelseType).orElse(null),
            filterRequest.fom(),
            filterRequest.tom());
        return Response.ok(dtoer).build();
    }

    private static ForespørselStatusDto mapStatus(no.nav.foreldrepenger.inntektsmelding.felles.ForespørselStatusDto status) {
        return switch (status) {
            case UNDER_BEHANDLING -> ForespørselStatusDto.UNDER_BEHANDLING;
            case FERDIG -> ForespørselStatusDto.FERDIG;
            case UTGÅTT -> ForespørselStatusDto.UTGÅTT;
        };
    }

    private static YtelseTypeDto mapYtelseType(no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> YtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseTypeDto.SVANGERSKAPSPENGER;
        };
    }

    private void sjekkErSystemkall() {
        tilgang.sjekkErSystembruker();
    }
}
