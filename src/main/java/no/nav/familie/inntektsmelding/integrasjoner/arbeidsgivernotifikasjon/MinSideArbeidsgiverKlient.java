package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import static no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonErrorHandler.handleError;
import static no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonErrorHandler.handleValidationError;

import java.net.http.HttpRequest;

import jakarta.enterprise.context.Dependent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLRequest;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResult;

import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "arbeidsgiver.notifikasjon.url", endpointDefault = "http://notifikasjon-produsent-api.fager/api/graphql", scopesProperty = "arbeidsgiver.notifikasjon.scopes", scopesDefault = "api://prod-gcp.fager.notifikasjon-produsent-api/.default")
class MinSideArbeidsgiverKlient {

    private static final Logger LOG = LoggerFactory.getLogger(MinSideArbeidsgiverKlient.class);

    private static final String ERROR_RESPONSE = "F-102030";

    private final RestClient restKlient;
    private final RestConfig restConfig;

    MinSideArbeidsgiverKlient() {
        this(RestClient.client());
    }

    public MinSideArbeidsgiverKlient(RestClient restKlient) {
        this.restKlient = restKlient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public String opprettSak(NySakMutationRequest request, NySakResultatResponseProjection projection) {
        LOG.info("FAGER: Opprett Sak");
        var resultat = query(new GraphQLRequest(request, projection), NySakMutationResponse.class).nySak();
        if (resultat instanceof NySakVellykket vellykket) {
            LOG.info("Vellykket opprettelse av sak");
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "opprettelse av ny sak");
        }
        throw ulovligTilstandException();
    }

    public String oppdaterSakTilleggsinformasjon(TilleggsinformasjonSakMutationRequest request,
                                                 TilleggsinformasjonSakResultatResponseProjection projection) {
        LOG.info("FAGER: Oppdater tillegsinformasjon på sak");
        var resultat = query(new GraphQLRequest(request, projection), TilleggsinformasjonSakMutationResponse.class).tilleggsinformasjonSak();
        if (resultat instanceof TilleggsinformasjonSakVellykket vellykket) {
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "oppdater sak tillegsinformasjon");
        }
        throw ulovligTilstandException();
    }

    public String opprettOppgave(NyOppgaveMutationRequest request, NyOppgaveResultatResponseProjection projection) {
        LOG.info("FAGER: Opprett Oppgave");
        var resultat = query(new GraphQLRequest(request, projection), NyOppgaveMutationResponse.class).nyOppgave();
        if (resultat instanceof NyOppgaveVellykket vellykket) {
            LOG.info("Vellykket opprettelse av oppgave");
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "opprettelse av ny oppgave");
        }
        throw ulovligTilstandException();
    }

    public String oppgaveUtført(OppgaveUtfoertMutationRequest request, OppgaveUtfoertResultatResponseProjection projection) {
        LOG.info("FAGER: Oppgave utført");
        var resultat = query(new GraphQLRequest(request, projection), OppgaveUtfoertMutationResponse.class).oppgaveUtfoert();
        if (resultat instanceof OppgaveUtfoertVellykket vellykket) {
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "sett oppgave utført");
        }
        throw ulovligTilstandException();
    }

    public String oppgaveUtgått(OppgaveUtgaattMutationRequest request, OppgaveUtgaattResultatResponseProjection projection) {
        LOG.info("FAGER: Oppgave utgått");
        var resultat = query(new GraphQLRequest(request, projection), OppgaveUtgaattMutationResponse.class).oppgaveUtgaatt();
        if (resultat instanceof OppgaveUtgaattVellykket vellykket) {
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "sett oppgave utgått");
        }
        throw ulovligTilstandException();
    }

    public String oppdaterSakStatus(NyStatusSakMutationRequest request, NyStatusSakResultatResponseProjection projection) {
        LOG.info("FAGER: Oppdater sak status");
        var resultat = query(new GraphQLRequest(request, projection), NyStatusSakMutationResponse.class).nyStatusSak();
        if (resultat instanceof NyStatusSakVellykket vellykket) {
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "ny status sak");
        }
        throw ulovligTilstandException();
    }

    public String slettSak(HardDeleteSakMutationRequest request, HardDeleteSakResultatResponseProjection projection) {
        LOG.info("FAGER: Utfører hard delete");
        var resultat = query(new GraphQLRequest(request, projection), HardDeleteSakMutationResponse.class).hardDeleteSak();
        if (resultat instanceof HardDeleteSakVellykket vellykket) {
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "hard delete sak");
        }
        throw ulovligTilstandException();
    }

    public String opprettBeskjedOgVarsling(NyBeskjedMutationRequest request, NyBeskjedResultatResponseProjection projection) {
        LOG.info("FAGER: Oppretter beskjed og ekstern varsling");
        var resultat = query(new GraphQLRequest(request, projection), NyBeskjedMutationResponse.class).nyBeskjed();
        if (resultat instanceof NyBeskjedVellykket vellykket) {
            LOG.info("Vellykket opprettelse av beskjed");
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "opprettelse av ny beskjed");
        }
        throw ulovligTilstandException();
    }

    private <T extends GraphQLResult<?>> T query(GraphQLRequest req, Class<T> clazz) {
        var method = new RestRequest.Method(RestRequest.WebMethod.POST, HttpRequest.BodyPublishers.ofString(req.toHttpJsonBody()));
        var restRequest = RestRequest.newRequest(method, restConfig.endpoint(), restConfig);
        var response = restKlient.sendReturnUnhandled(restRequest);
        if (LOG.isInfoEnabled()) {
            LOG.info("FAGER: Svar med code: {} og body: {}", response.statusCode(), response.body());
        }
        var res = handleResponse(response.body(), clazz);
        if (res == null) {
            throw new TekniskException("F-FAGER", "Mottok null som respons fra fager");
        }
        if (res.hasErrors()) {
            return handleError(res.getErrors(), restConfig.endpoint(), ERROR_RESPONSE);
        }
        return res;
    }

    private <T> T handleResponse(String response, Class<T> clazz) {
        if (response == null) {
            return null;
        }
        if (clazz.isAssignableFrom(String.class)) {
            return clazz.cast(response);
        }
        LOG.info("FAGER: Response: {} til class {}", response, clazz);
        return DefaultJsonMapper.fromJson(response, clazz);
    }

    private static IllegalStateException ulovligTilstandException() {
        return new IllegalStateException("Utviklerfeil: Ulovlig tilstand.");
    }

    private static void loggFeilmelding(Error feil, String action) {
        handleValidationError("Funksjonellfeil", feil.getFeilmelding(), action);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [endpoint=" + restConfig.endpoint() + "]";
    }

}
