package no.nav.foreldrepenger.inntektsmelding.imapi.rest.inntektsmelding;

import java.util.List;
import java.util.Optional;
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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingResponse;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.InntektsmeldingFilterRequest;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingRequest;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.tjenester.InntektsmeldingKontraktMapper;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.tjenester.InntektsmeldingApiMapper;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.tjenester.InntektsmeldingApiMottakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.foreldrepenger.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.foreldrepenger.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.vedtak.feil.Feilkode;
import no.nav.vedtak.server.rest.FeilUtils;

@AutentisertMedAzure
@RequestScoped
@Transactional
@Path(InntektsmeldingApiRest.BASE_PATH)
public class InntektsmeldingApiRest {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingApiRest.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");

    public static final String BASE_PATH = "/imapi/inntektsmelding";
    private Tilgang tilgangskontroll;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private InntektsmeldingApiMottakTjeneste inntektsmeldingMottakTjeneste;
    private PersonTjeneste personTjeneste;
    private InntektsmeldingKontraktMapper inntektsmeldingKontraktMapper;

    InntektsmeldingApiRest() {
        // CDI
    }

    @Inject
    public InntektsmeldingApiRest(InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                  InntektsmeldingApiMottakTjeneste inntektsmeldingMottakTjeneste,
                                  PersonTjeneste personTjeneste, Tilgang tilgangskontroll,
                                  InntektsmeldingKontraktMapper inntektsmeldingKontraktMapper) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.tilgangskontroll = tilgangskontroll;
        this.inntektsmeldingMottakTjeneste = inntektsmeldingMottakTjeneste;
        this.personTjeneste = personTjeneste;
        this.inntektsmeldingKontraktMapper = inntektsmeldingKontraktMapper;
    }

    @GET
    @Path("/hent/{inntektsmeldingUuid}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter en tidligere innsendt inntektsmelding", tags = "ekstern-api")
    @Tilgangskontrollert
    public Response hentInntektsmelding(@NotNull @Valid @PathParam("inntektsmeldingUuid") UUID inntektsmeldingUuid) {
        tilgangskontroll.sjekkErSystembruker();
        LOG.trace("Henter inntektsmelding med UUID: {}", inntektsmeldingUuid);
        var inntektsmelding = inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingUuid);

        if (inntektsmelding == null) {
            LOG.info("Fant ingen inntektsmelding for UUID: {}", inntektsmeldingUuid);
            return FeilUtils.responseFra(Response.Status.NOT_FOUND, Feilkode.IKKE_FUNNET, "Inntektsmelding med %s id finnes ikke.".formatted(inntektsmeldingUuid));
        }

        var kontrakt = inntektsmeldingKontraktMapper.mapTilKontrakt(inntektsmelding);
        return Response.ok(kontrakt).build();    }

    @POST
    @Path("/send-inntektsmelding")
    @Tilgangskontrollert
    public SendInntektsmeldingResponse sendEksternInntektsmelding(@NotNull @Valid SendInntektsmeldingRequest request) {
        tilgangskontroll.sjekkErSystembruker();
        LOG.info("Mottatt inntektsmelding fra ekstern kilde for forespørselUuid {} ", request.foresporselUuid());

        var aktørId = Optional.ofNullable(request.fødselsnummer())
            .flatMap(ident -> personTjeneste.finnAktørIdForIdent(new PersonIdent(ident.fnr())));

        if (aktørId.isEmpty()) {
            SECURE_LOG.error("Finner ikke aktørId for fødselsnummer {}", request.fødselsnummer());
            return new SendInntektsmeldingResponse(false, null,
                "Finner ikke informasjon for fødselsnummer. Sjekk at fødselsnummer er korrekt");
        }

        var mottattInntektsmelding = InntektsmeldingApiMapper.mapTilDto(request, aktørId.get());
        return inntektsmeldingMottakTjeneste.mottaInntektsmelding(mottattInntektsmelding, request.foresporselUuid());
    }

    @POST
    @Path("/hent/inntektsmeldinger")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter inntektsmeldinger basert på filter", tags = "ekstern-api")
    @Tilgangskontrollert
    public Response hentInntektsmeldinger(@Valid @NotNull InntektsmeldingFilterRequest filterRequest) {
        tilgangskontroll.sjekkErSystembruker();

        List<HentInntektsmeldingResponse> responsListe;

        if (filterRequest.forespørselUuid() != null) {
            responsListe = inntektsmeldingTjeneste.hentInntektsmeldinger(filterRequest.forespørselUuid()).stream()
                .map(inntektsmeldingKontraktMapper::mapTilKontrakt)
                .toList();
        } else {
            var aktørId = Optional.ofNullable(filterRequest.fnr())
                .flatMap(fnr -> personTjeneste.finnAktørIdForIdent(new PersonIdent(fnr.fnr())))
                .map(a -> new AktørIdEntitet(a.getAktørId()))
                .orElse(null);

            var ytelseType = Optional.ofNullable(filterRequest.ytelseType())
                .map(InntektsmeldingApiRest::mapYtelseType)
                .orElse(null);

            responsListe = inntektsmeldingTjeneste.hentInntektsmeldingerFraFilter(
                    filterRequest.orgnr().orgnr(),
                    aktørId,
                    ytelseType,
                    filterRequest.fom(),
                    filterRequest.tom()).stream()
                .map(inntektsmeldingKontraktMapper::mapTilKontrakt)
                .toList();
        }

        return Response.ok(responsListe).build();
    }

    private static Ytelsetype mapYtelseType(YtelseTypeDto ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> Ytelsetype.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> Ytelsetype.SVANGERSKAPSPENGER;
        };
    }
}
