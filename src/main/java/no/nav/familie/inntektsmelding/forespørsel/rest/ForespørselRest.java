package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.util.ArrayList;
import java.util.List;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

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

        if (request.organisasjonsnumre() != null){
            if (request.organisasjonsnumre().isEmpty()) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }

            LOG.info("Mottok forespørsel om inntektsmeldingoppgave på fagsakSaksnummer {} for orgnumrene: {} ", request.fagsakSaksnummer(), request.organisasjonsnumre());
            List<OpprettForespørselResponsNy.OrganisasjonsnummerMedStatus> organisasjonsnumreMedStatus = new ArrayList<>();
            request.organisasjonsnumre().forEach(organisasjonsnummer -> {
                var bleForespørselOpprettet = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(request.skjæringstidspunkt(),
                    KodeverkMapper.mapYtelsetype(request.ytelsetype()),
                    new AktørIdEntitet(request.aktørId().id()),
                    new OrganisasjonsnummerDto(organisasjonsnummer.orgnr()),
                    request.fagsakSaksnummer(),
                    request.førsteUttaksdato());

                if (ForespørselResultat.FORESPØRSEL_OPPRETTET.equals(bleForespørselOpprettet)) {
                    MetrikkerTjeneste.loggForespørselOpprettet(KodeverkMapper.mapYtelsetype(request.ytelsetype()));
                }
                organisasjonsnumreMedStatus.add(new OpprettForespørselResponsNy.OrganisasjonsnummerMedStatus(organisasjonsnummer, bleForespørselOpprettet));
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

        forespørselBehandlingTjeneste.lukkForespørsel(request.fagsakSaksnummer(), request.orgnummer(), request.skjæringstidspunkt());
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

        var resultat = forespørselBehandlingTjeneste.opprettNyBeskjedMedEksternVarsling(request.fagsakSaksnummer(), request.orgnummer());
        return Response.ok(new SendNyBeskjedResponse(resultat)).build();
    }

    @POST
    @Path("/sett-til-utgatt")
    @Tilgangskontrollert
    public Response settForespørselTilUtgått(@Valid @NotNull LukkForespørselRequest request) {
        LOG.info("Setter forespørsel for fagsakSaksnummer {} til utgått", request.fagsakSaksnummer());

        sjekkErSystemkall();

        forespørselBehandlingTjeneste.settForespørselTilUtgått(request.fagsakSaksnummer(), request.orgnummer(), request.skjæringstidspunkt());
        return Response.ok().build();
    }

    private void sjekkErSystemkall() {
        tilgang.sjekkErSystembruker();
    }

    private void sjekkErSaksbehandlerkall() {
        tilgang.sjekkAtAnsattHarRollenSaksbehandler();
    }

}

