package no.nav.familie.inntektsmelding.imdialog.rest;

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

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.tjenester.GrunnlagDtoTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.fpsak.FpsakTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.vedtak.exception.FunksjonellException;

@AutentisertMedTokenX
@RequestScoped
@Transactional
@Consumes(MediaType.APPLICATION_JSON)
@Path(ArbeidsgiverinitiertDialogRest.BASE_PATH)
public class ArbeidsgiverinitiertDialogRest {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsgiverinitiertDialogRest.class);

    public static final String BASE_PATH = "/arbeidsgiverinitiert";
    private static final String HENT_ARBEIDSFORHOLD = "/arbeidsforhold";
    private static final String HENT_ARBEIDSGIVERE_FOR_UREGISTRERT = "/arbeidsgivereForUregistrert";
    private static final String HENT_OPPLYSNINGER = "/opplysninger";

    private GrunnlagDtoTjeneste grunnlagDtoTjeneste;
    private FpsakTjeneste fpsakTjeneste;

    ArbeidsgiverinitiertDialogRest() {
        // CDI
    }

    @Inject
    public ArbeidsgiverinitiertDialogRest(GrunnlagDtoTjeneste grunnlagDtoTjeneste,
                                          FpsakTjeneste fpsakTjeneste) {
        this.grunnlagDtoTjeneste = grunnlagDtoTjeneste;
        this.fpsakTjeneste = fpsakTjeneste;
    }

    @POST
    @Path(HENT_ARBEIDSFORHOLD)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentArbeidsforhold(@Valid @NotNull HentArbeidsgiverRequest request) {
        LOG.info("Henter arbeidsforhold for søker {}", request.fødselsnummer());
        var personInfo = grunnlagDtoTjeneste.finnPersoninfo(request.fødselsnummer(), request.ytelseType());
        if (personInfo == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var aktørId = personInfo.aktørId();
        var eksisterendeForepørslersisteTreÅr  = grunnlagDtoTjeneste.finnForespørslerSisteTreÅr(request.ytelseType(), request.førsteFraværsdag(), aktørId);
        if (eksisterendeForepørslersisteTreÅr.isEmpty()) {
            LOG.info("Fant ikke forespørsel siste tre år for aktør {}, spør fpsak.", aktørId);
            var finnesYtelseIFpsak = fpsakTjeneste.harAktørSakIFagsystem(aktørId, request.ytelseType());
            if (!finnesYtelseIFpsak) {
                var tekst = String.format("Du kan ikke sende inn inntektsmelding på %s for denne personen med aktør id %s",  request.ytelseType(), personInfo.aktørId());
                throw new FunksjonellException("INGEN_SAK_FUNNET",tekst, null, null);
            }
        }
        var dto = grunnlagDtoTjeneste.finnArbeidsforholdForFnr(personInfo, request.førsteFraværsdag());
        return dto.map(d ->Response.ok(d).build()).orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path(HENT_ARBEIDSGIVERE_FOR_UREGISTRERT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentArbeidsgivereforUregistrert(@Valid @NotNull ArbeidsgiverinitiertDialogRest.HentArbeidsgivereUregistrert request) {
        LOG.info("Henter personinformasjon for {}, og organisasjoner som innsender har tilgang til", request.fødselsnummer());
        var personInfo = grunnlagDtoTjeneste.finnPersoninfo(request.fødselsnummer(), request.ytelseType());
        if (personInfo == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var dto = grunnlagDtoTjeneste.hentSøkerinfoOgOrganisasjonerArbeidsgiverHarTilgangTil(personInfo);
        return dto.map(d -> Response.ok(d).build()).orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    public record HentArbeidsgivereUregistrert(@Valid @NotNull PersonIdent fødselsnummer, @Valid @NotNull Ytelsetype ytelseType) {}

    @POST
    @Path(HENT_OPPLYSNINGER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentOpplysninger(@Valid @NotNull OpplysningerRequestDto request) {
        LOG.info("Henter opplysninger for søker {}", request.fødselsnummer());
        var dto = grunnlagDtoTjeneste.lagArbeidsgiverinitiertDialogDto(request.fødselsnummer(), request.ytelseType(), request.førsteFraværsdag(), request.organisasjonsnummer().orgnr());
        return Response.ok(dto).build();
    }
}
