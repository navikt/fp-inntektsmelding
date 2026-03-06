package no.nav.familie.inntektsmelding.forespørsel.rest.ekstern;

import static no.nav.familie.inntektsmelding.forespørsel.rest.ekstern.ForespørselEksternRest.BASE_PATH;

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
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselStatusDto;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

@AutentisertMedAzure
@ApplicationScoped
@Transactional
@Path(BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ForespørselEksternRest {
    public static final String BASE_PATH = "/foresporsel-ekstern";
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselEksternRest.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private Tilgang tilgang;

    ForespørselEksternRest() {
        // Kun for CDI-proxy
    }

    @Inject
    public ForespørselEksternRest(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste, Tilgang tilgang) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.tilgang = tilgang;
    }

    @GET
    @Path("/hent/{forespørselUuid}")
    @Tilgangskontrollert
    public Response hentForespørsel(@Valid @PathParam("forespørselUuid") UUID forespørselUuId) {
        sjekkErSystemkall();

        var forespørselEntitet = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuId);

        if (forespørselEntitet.isEmpty()) {
            LOG.warn("Forespørsel med uuid {} finnes ikke", forespørselUuId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        var forespørselDto = forespørselEntitet.map(ForespørselEksternRest::mapTilDto);

        return forespørselDto.map(Response::ok).orElse(Response.status(Response.Status.NO_CONTENT)).build();
    }

    @GET
    @Path("/hent/foresporsler")
    @Tilgangskontrollert
    public Response hentForespørsler(@Valid @NotNull ForespørselFilterRequest filterRequest) {
        sjekkErSystemkall();
        var resultat = forespørselBehandlingTjeneste.hentForespørsler(filterRequest.orgnr(),
            filterRequest.fnr(),
            filterRequest.status(),
            filterRequest.ytelseType(),
            filterRequest.fom(),
            filterRequest.tom());
        var dtoer = resultat.stream().map(ForespørselEksternRest::mapTilDto).toList();
        return Response.ok(dtoer).build();
    }

    private static ForespørselDto mapTilDto(ForespørselEntitet fp) {
        return new ForespørselDto(fp.getUuid(),
            new OrganisasjonsnummerDto(fp.getOrganisasjonsnummer()),
            new AktørIdDto(fp.getAktørId().getAktørId()),
            fp.getFørsteUttaksdato(),
            //todo skal det være mulig å sende inn arbeidsgiverinitert fp gjennom api. Det er kun da denne kan være null
            fp.getSkjæringstidspunkt().orElse(null),
            KodeverkMapper.mapForespørselStatus(fp.getStatus()),
            KodeverkMapper.mapYtelsetype(fp.getYtelseType()));
    }

    protected record ForespørselFilterRequest(@NotNull @Valid OrganisasjonsnummerDto orgnr,
                                              @Pattern(regexp = "^\\d{11}$") String fnr,
                                              @Valid ForespørselStatusDto status,
                                              @Valid YtelseTypeDto ytelseType,
                                              LocalDate fom,
                                              LocalDate tom) {
    }

    private void sjekkErSystemkall() {
        tilgang.sjekkErSystembruker();
    }
}
