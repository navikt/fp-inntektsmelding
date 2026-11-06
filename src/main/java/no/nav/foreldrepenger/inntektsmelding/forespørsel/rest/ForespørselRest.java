package no.nav.foreldrepenger.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ArbeidsgiverMedStatus;
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
import no.nav.foreldrepenger.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.YtelseTypeDto;

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
    @Path("/opprett-en")
    @Tilgangskontrollert
    public Response opprettEnForespørsel(@Valid @NotNull OpprettEnForespørselRequest request) {
        sjekkErSystemkall();

        LOG.info("Mottok beskjed fra fpsak om å opprette forespørsel på {} med skjæringstidspunkt {} og første uttaksdato {}",
            request.fagsakSaksnummer(),
            request.skjæringstidspunkt(),
            request.førsteUttaksdato());
        var organisasjonsnummer = request.organisasjonsnummerMedStatus() == null ? request.orgnummer() : request.organisasjonsnummerMedStatus().orgnummer();
        var erInntektsmeldingMottatt = request.organisasjonsnummerMedStatus() == null ? false : request.organisasjonsnummerMedStatus().erInntektsmeldingMottatt();
        var resultat = håndterInnkommendeForespørsel(request.skjæringstidspunkt(),
            request.ytelsetype(),
            request.aktørId(),
            organisasjonsnummer,
            erInntektsmeldingMottatt,
            request.fagsakSaksnummer(),
            request.førsteUttaksdato());
        return Response.ok(new OpprettForespørselRespons.OrganisasjonsnummerMedStatus(request.orgnummer(), resultat)).build();
    }

    @POST
    @Path("/opprett-komplett")
    @Tilgangskontrollert
    public Response opprettForespørslerKomplettListe(@Valid @NotNull OpprettFlereForespørslerRequest request) {
        sjekkErSystemkall();

        LOG.info("Mottok komplett liste fra fpsak for forespørsler på {} med skjæringstidspunkt {} og første uttaksdato {}",
            request.fagsakSaksnummer(),
            request.skjæringstidspunkt(),
            request.førsteUttaksdato());
        var ytelsetype = KodeverkMapper.mapYtelsetype(request.ytelsetype());
        var orgnrMedStatus = hentListeMedOrgnrOgStatus(request);
        var resultater = forespørselBehandlingTjeneste.håndterKomplettListeMedForespørsler(request.skjæringstidspunkt(),
            ytelsetype,
            AktørId.fra(request.aktørId().id()),
            orgnrMedStatus,
            Saksnummer.fra(request.fagsakSaksnummer().saksnr()),
            request.førsteUttaksdato());

        var organisasjonsnumreMedStatus = resultater.stream()
            .map(par -> new OpprettForespørselRespons.OrganisasjonsnummerMedStatus(
                new OrganisasjonsnummerDto(par.arbeidsgiver().orgnr()),
                par.resultat()))
            .toList();
        resultater.forEach(par -> loggOpprettetMetrikk(request.ytelsetype(), par.resultat()));
        return Response.ok(new OpprettForespørselRespons(organisasjonsnumreMedStatus)).build();
    }

    private List<ArbeidsgiverMedStatus> hentListeMedOrgnrOgStatus(OpprettFlereForespørslerRequest request) {

        if (request.organisasjonsnummerMedStatus() != null) {
            if (request.organisasjonsnummer() != null) {
                var orgnumreUtenStatus = request.organisasjonsnummer().stream().map(OrganisasjonsnummerDto::orgnr).toList();
                var orgnumreMedStatus = request.organisasjonsnummerMedStatus().stream().map(p -> p.orgnummer().orgnr()).toList();
                var unikeOrgnumreUtenStatus = new HashSet<>(orgnumreUtenStatus);
                var unikeOrgnumreMedStatus = new HashSet<>(orgnumreMedStatus);
                if (unikeOrgnumreUtenStatus.size() != orgnumreUtenStatus.size()
                    || unikeOrgnumreMedStatus.size() != orgnumreMedStatus.size()
                    || !unikeOrgnumreUtenStatus.equals(unikeOrgnumreMedStatus)) {
                    throw new BadRequestException("Listene må inneholde samme organisasjonsnumre uten duplikater.");
                }
            }
            return request.organisasjonsnummerMedStatus().stream().map(p -> new ArbeidsgiverMedStatus(Arbeidsgiver.fra(p.orgnummer().orgnr()), p.erInntektsmeldingMottatt())).toList();
        }
        return request.organisasjonsnummer().stream().map(r -> new ArbeidsgiverMedStatus(Arbeidsgiver.fra(r.orgnr()), false)).toList();
    }

    private void loggOpprettetMetrikk(YtelseTypeDto ytelsetype, ForespørselResultat resultat) {
        if (ForespørselResultat.FORESPØRSEL_OPPRETTET.equals(resultat)) {
            MetrikkerTjeneste.loggForespørselOpprettet(KodeverkMapper.mapYtelsetype(ytelsetype));
        }
    }

    private ForespørselResultat håndterInnkommendeForespørsel(LocalDate skjæringstidspunkt,
                                                              YtelseTypeDto ytelsetype,
                                                              AktørIdDto aktørId,
                                                              OrganisasjonsnummerDto organisasjonsnummer,
                                                              boolean erInntektsmeldingMottatt,
                                                              SaksnummerDto fagsakSaksnummer,
                                                              LocalDate førsteUttaksdato) {
        // Hvis IM er mottatt skal vi bare opprette en forespørsel i dialogporten / min side -arbeidsgiver slik at de skal finne igjen saken i fremtiden for å kunne endre inntektsmelding
        if (erInntektsmeldingMottatt) {
            var resultat = forespørselBehandlingTjeneste.håndterForespørselSomUmibddelbartSkalLukkes(skjæringstidspunkt,
                KodeverkMapper.mapYtelsetype(ytelsetype),
                AktørId.fra(aktørId.id()),
                Arbeidsgiver.fra(organisasjonsnummer.orgnr()),
                Saksnummer.fra(fagsakSaksnummer.saksnr()),
                førsteUttaksdato);
            loggOpprettetMetrikk(ytelsetype, resultat);
            return resultat;
        } else {
            var resultat = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(skjæringstidspunkt,
                KodeverkMapper.mapYtelsetype(ytelsetype),
                AktørId.fra(aktørId.id()),
                Arbeidsgiver.fra(organisasjonsnummer.orgnr()),
                Saksnummer.fra(fagsakSaksnummer.saksnr()),
                førsteUttaksdato);
            loggOpprettetMetrikk(ytelsetype, resultat);
            return resultat;
        }
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
