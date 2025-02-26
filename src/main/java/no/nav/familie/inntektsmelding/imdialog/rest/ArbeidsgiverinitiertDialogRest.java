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

import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.foreldrepenger.konfig.Environment;
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
    private static final String HENT_OPPLYSNINGER = "/opplysninger";

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private PersonTjeneste personTjeneste;
    private boolean erProd = true;

    ArbeidsgiverinitiertDialogRest() {
        // CDI
    }

    @Inject
    public ArbeidsgiverinitiertDialogRest(InntektsmeldingTjeneste inntektsmeldingTjeneste, PersonTjeneste personTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.personTjeneste = personTjeneste;
        this.erProd = Environment.current().isProd();
    }

    @POST
    @Path(HENT_ARBEIDSFORHOLD)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentArbeidsforhold(@Valid @NotNull HentArbeidsgiverRequest request) {
        if (erProd) {
            throw new IllegalStateException("Ugyldig kall på restpunkt som ikke er lansert");
        }

        var ytelsetype = request.ytelseType();
        var fødselsnummer = request.fødselsnummer();
        var førsteFraværsdag = request.førsteFraværsdag();
        // TODO Skal vi sjekke noe mtp kode 6/7 her?
        var personInfo = personTjeneste.hentPersonFraIdent(fødselsnummer, ytelsetype);
        if (personInfo == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        var aktørId = personInfo.aktørId();
        var eksisterendeForepørslersisteTreÅr = inntektsmeldingTjeneste.finnForespørslerSisteTreÅr(ytelsetype, førsteFraværsdag, aktørId);
        if (eksisterendeForepørslersisteTreÅr.isEmpty()) {
            var tekst = String.format("Du kan ikke sende inn inntektsmelding på %s for denne personen med aktør id %s",  ytelsetype, personInfo.aktørId());
            throw new FunksjonellException("INGEN_SAK_FUNNET",tekst, null, null);
        }

        LOG.info("Henter arbeidsforhold for søker {}", request.fødselsnummer());
        var dto = inntektsmeldingTjeneste.finnArbeidsforholdForFnr(personInfo, førsteFraværsdag);
        return dto.map(d ->Response.ok(d).build()).orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path(HENT_OPPLYSNINGER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentOpplysninger(@Valid @NotNull OpplysningerRequestDto request) {
        if (erProd) {
            throw new IllegalStateException("Ugyldig kall på restpunkt som ikke er lansert");
        }
        LOG.info("Henter opplysninger for søker {}", request.fødselsnummer());
        var dto = inntektsmeldingTjeneste.lagArbeidsgiverinitiertDialogDto(request.fødselsnummer(), request.ytelseType(), request.førsteFraværsdag(), request.organisasjonsnummer());
        return Response.ok(dto).build();
    }
}
