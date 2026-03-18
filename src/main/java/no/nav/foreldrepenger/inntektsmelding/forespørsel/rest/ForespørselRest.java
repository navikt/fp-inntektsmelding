package no.nav.foreldrepenger.inntektsmelding.forespørsel.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
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

import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.foreldrepenger.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.KodeverkMapper;

@AutentisertMedAzure
@ApplicationScoped
@Transactional
@Path(ForespørselRest.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ForespørselRest {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselRest.class);
    public static final String BASE_PATH = "/foresporsel";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private Tilgang tilgang;

    ForespørselRest() {
        // Kun for CDI-proxy
    }

    @Inject
    public ForespørselRest(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste, Tilgang tilgang) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.tilgang = tilgang;
    }

    @POST
    @Path("/opprett")
    @Tilgangskontrollert
    public Response opprettForespørsel(@Valid @NotNull OpprettForespørselRequest request) {
        sjekkErSystemkall();

        if (request.organisasjonsnumre() != null) {
            if (request.organisasjonsnumre().isEmpty()) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            var skjæringstidspunkt = request.skjæringstidspunkt();
            var førsteUttaksdato = request.førsteUttaksdato();
            var fagsakSaksnummer = request.fagsakSaksnummer();
            LOG.info(
                "Mottok beskjed fra fpsak om å opprette forespørsel på {} med skjæringstidspunkt {} og første uttaksdato {} på: {} ",
                fagsakSaksnummer,
                skjæringstidspunkt,
                førsteUttaksdato,
                request.organisasjonsnumre());
            List<OpprettForespørselResponsNy.OrganisasjonsnummerMedStatus> organisasjonsnumreMedStatus = new ArrayList<>();

            request.organisasjonsnumre().forEach(organisasjonsnummer -> {
                var bleForespørselOpprettet = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(skjæringstidspunkt,
                    KodeverkMapper.mapYtelsetype(request.ytelsetype()),
                    AktørId.fra(request.aktørId().id()),
                    Arbeidsgiver.fra(organisasjonsnummer.orgnr()),
                    Saksnummer.fra(fagsakSaksnummer.saksnr()),
                    førsteUttaksdato);

                if (ForespørselResultat.FORESPØRSEL_OPPRETTET.equals(bleForespørselOpprettet)) {
                    MetrikkerTjeneste.loggForespørselOpprettet(KodeverkMapper.mapYtelsetype(request.ytelsetype()));
                }

                organisasjonsnumreMedStatus.add(new OpprettForespørselResponsNy.OrganisasjonsnummerMedStatus(organisasjonsnummer,
                    bleForespørselOpprettet));
            });
            return Response.ok(new OpprettForespørselResponsNy(organisasjonsnumreMedStatus)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @POST
    @Path("/lukk")
    @Tilgangskontrollert
    public Response lukkForespørsel(@Valid @NotNull LukkForespørselRequest request) {
        LOG.info("Lukk forespørsel for fagsakSaksnummer {} med orgnummer {} og skjæringstidspunkt {}",
            request.fagsakSaksnummer(),
            request.orgnummer(),
            request.skjæringstidspunkt());

        sjekkErSystemkall();

        forespørselBehandlingTjeneste.lukkForespørsel(
            Saksnummer.fra(request.fagsakSaksnummer().saksnr()),
            Optional.ofNullable(request.orgnummer()).map(OrganisasjonsnummerDto::orgnr).map(Arbeidsgiver::new).orElse(null),
            request.skjæringstidspunkt());
        return Response.ok().build();
    }

    /**
     * Tjeneste for å opprette en ny beskjed på en eksisterende forespørsel.
     * Vil opprette ny beskjed som er synlig under saken i min side arbeidsgiver, samt sende ut et eksternt varsel
     *
     * @param request
     * @return
     */
    @POST
    @Path("/ny-beskjed")
    @Tilgangskontrollert
    public Response sendNyBeskjedOgVarsel(@Valid @NotNull NyBeskjedRequest request) {
        LOG.info("Ny beskjed på aktiv forespørsel for fagsakSaksnummer {} med orgnummer {}",
            request.fagsakSaksnummer(),
            request.orgnummer());

        sjekkErSaksbehandlerkall();

        var resultat = forespørselBehandlingTjeneste.opprettNyBeskjedMedEksternVarsling(
            Saksnummer.fra(request.fagsakSaksnummer().saksnr()),
            Arbeidsgiver.fra(request.orgnummer().orgnr()));
        return Response.ok(new SendNyBeskjedResponse(resultat)).build();
    }

    @POST
    @Path("/sett-til-utgatt")
    @Tilgangskontrollert
    public Response settForespørselTilUtgått(@Valid @NotNull LukkForespørselRequest request) {
        LOG.info("Setter forespørsel for fagsakSaksnummer {} til utgått", request.fagsakSaksnummer());

        sjekkErSystemkall();

        forespørselBehandlingTjeneste.settForespørselTilUtgått(
            Saksnummer.fra(request.fagsakSaksnummer().saksnr()),
            Optional.ofNullable(request.orgnummer()).map(OrganisasjonsnummerDto::orgnr).map(Arbeidsgiver::new).orElse(null),
            request.skjæringstidspunkt());
        return Response.ok().build();
    }

    private void sjekkErSystemkall() {
        tilgang.sjekkErSystembruker();
    }

    private void sjekkErSaksbehandlerkall() {
        tilgang.sjekkAtAnsattHarRollenSaksbehandler();
    }
}

