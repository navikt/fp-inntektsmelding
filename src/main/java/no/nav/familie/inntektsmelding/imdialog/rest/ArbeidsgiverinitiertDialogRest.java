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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.tjenester.GrunnlagDtoTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.fpsak.FpsakKlient;
import no.nav.familie.inntektsmelding.integrasjoner.fpsak.FpsakTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.FunksjonellException;

import java.time.LocalDate;

@AutentisertMedTokenX
@RequestScoped
@Transactional
@Consumes(MediaType.APPLICATION_JSON)
@Path(ArbeidsgiverinitiertDialogRest.BASE_PATH)
public class ArbeidsgiverinitiertDialogRest {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsgiverinitiertDialogRest.class);
    private static final Environment ENV = Environment.current();

    public static final String BASE_PATH = "/arbeidsgiverinitiert";
    private static final String HENT_ARBEIDSFORHOLD = "/arbeidsforhold";
    private static final String HENT_ARBEIDSGIVERE_FOR_UREGISTRERT = "/arbeidsgivereForUregistrert";
    private static final String HENT_OPPLYSNINGER = "/opplysninger";
    private static final String HENT_OPPLYSNINGER_UREGISTRERT = "/opplysningerUregistrert";

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
        var eksisterendeForepørslersisteTreÅr = grunnlagDtoTjeneste.finnForespørslerSisteTreÅr(request.ytelseType(),
            request.førsteFraværsdag(),
            aktørId);
        if (eksisterendeForepørslersisteTreÅr.isEmpty()) {
            LOG.info("Fant ikke forespørsel siste tre år for aktør {}, spør fpsak.", aktørId);

            var infoOmSakRespons = fpsakTjeneste.henterInfoOmSakIFagsystem(aktørId, request.ytelseType());
            var finnesYtelseIFpsak = FpsakKlient.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING.equals(infoOmSakRespons.statusInntektsmelding());

            if (!finnesYtelseIFpsak) {
                var tekst = String.format("Du kan ikke sende inn inntektsmelding på %s for denne personen med aktør id %s",
                    request.ytelseType(),
                    personInfo.aktørId());
                throw new FunksjonellException("INGEN_SAK_FUNNET", tekst, null, null);
            }
        }
        var dto = grunnlagDtoTjeneste.finnArbeidsforholdForFnr(personInfo, request.førsteFraværsdag());
        return dto.map(d -> Response.ok(d).build()).orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path(HENT_ARBEIDSGIVERE_FOR_UREGISTRERT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentArbeidsgivereforUregistrert(@Valid @NotNull ArbeidsgiverinitiertDialogRest.HentArbeidsgivereUregistrert request) {
        if (ENV.isProd()) {
            throw new IllegalStateException("Funksjonen er ikke tilgjengelig i prod");
        }
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
        var dto = grunnlagDtoTjeneste.lagArbeidsgiverinitiertNyansattDialogDto(request.fødselsnummer(), request.ytelseType(), request.førsteFraværsdag(), request.organisasjonsnummer().orgnr());
        return Response.ok(dto).build();
    }

    @POST
    @Path(HENT_OPPLYSNINGER_UREGISTRERT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentOpplysningerUregistrert(@Valid @NotNull OpplysningerUregistrertRequestDto request) {
        if (ENV.isProd()) {
            throw new IllegalStateException("Funksjonen er ikke tilgjengelig i prod");
        }
        LOG.info("Henter opplysninger for søker {}", request.fødselsnummer());

        var personInfo = grunnlagDtoTjeneste.finnPersoninfo(request.fødselsnummer(), request.ytelseType());
        if (personInfo == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var aktørId = personInfo.aktørId();

        var infoOmsak = fpsakTjeneste.henterInfoOmSakIFagsystem(aktørId, request.ytelseType());

        if (!infoOmsak.statusInntektsmelding().equals(FpsakKlient.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING)) {
            if (infoOmsak.statusInntektsmelding().equals(FpsakKlient.StatusSakInntektsmelding.SØKT_FOR_TIDLIG)) {
                var ytelseTekst = request.ytelseType().equals(Ytelsetype.FORELDREPENGER) ? "foreldrepenger" : "svangerskapspenger";
                var tekst = String.format("Du kan ikke sende inn inntektsmelding før fire uker før personen med aktør id %s starter %s ",
                    personInfo.aktørId(),
                    ytelseTekst);
                throw new FunksjonellException("SENDT_FOR_TIDLIG", tekst, null, null);
            } else {
                var tekst = String.format("Du kan ikke sende inn inntektsmelding på %s for denne personen med aktør id %s",
                    request.ytelseType(),
                    personInfo.aktørId());
                throw new FunksjonellException("INGEN_SAK_FUNNET", tekst, null, null);
            }
        } else {
            var førsteUttaksdato = infoOmsak.førsteUttaksdato();
            var finnesOrgnummerIAaReg = grunnlagDtoTjeneste.finnesOrgnummerIAaregPåPerson(personInfo.fødselsnummer(),
                request.organisasjonsnummer().orgnr(),
                førsteUttaksdato);
            if (finnesOrgnummerIAaReg) {
                var tekst = "Det finnes rapportering i aa-registeret på organisasjonsnummeret. Nav vil be om inntektsmelding når vi trenger det ";
                throw new FunksjonellException("ORGNR_FINNES_I_AAREG", tekst, null, null);
            }

            var dto = grunnlagDtoTjeneste.lagArbeidsgiverinitiertUregistrertDialogDto(request.fødselsnummer(),
                request.ytelseType(),
                førsteUttaksdato,
                request.organisasjonsnummer().orgnr(),
                infoOmsak.skjæringstidspunkt());
            return Response.ok(dto).build();
        }
    }
}
