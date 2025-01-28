package no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent;

import java.time.YearMonth;
import java.util.Collections;

import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.tjenester.aordningen.inntektsinformasjon.Aktoer;
import no.nav.tjenester.aordningen.inntektsinformasjon.request.HentInntektListeBolkRequest;
import no.nav.tjenester.aordningen.inntektsinformasjon.response.HentInntektListeBolkResponse;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "hentinntektlistebolk.url", endpointDefault = "https://app.adeo.no/inntektskomponenten-ws/rs/api/v1/hentinntektlistebolk", scopesProperty = "hentinntektlistebolk.scopes", scopesDefault = "api://prod-fss.team-inntekt.inntektskomponenten/.default")
public class InntektskomponentKlient {
    private static final Logger LOG = LoggerFactory.getLogger(InntektskomponentKlient.class);
    private static final YearMonth INNTK_TIDLIGSTE_DATO = YearMonth.of(2015, 7);

    private final RestClient restClient;
    private final RestConfig restConfig;

    public InntektskomponentKlient() {
        this(RestClient.client());
    }

    public InntektskomponentKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public HentInntektListeBolkResponse finnInntekt(FinnInntektRequest finnInntektRequest) {
        var request = lagRequest(finnInntektRequest);
        LOG.info("Henter inntekt");

        HentInntektListeBolkResponse response;
        try {
            response = restClient.send(request, HentInntektListeBolkResponse.class);
        } catch (RuntimeException e) {
            throw new IntegrasjonException("FP-824246",
                "Feil ved kall til inntektstjenesten. Meld til #team_registre og #produksjonshendelser hvis dette skjer over lengre tidsperiode.", e);
        }
        return response;
    }

    private RestRequest lagRequest(FinnInntektRequest finnInntektRequest) {
        var request = new HentInntektListeBolkRequest();

        request.setIdentListe(Collections.singletonList(Aktoer.newAktoerId(finnInntektRequest.aktørId())));
        request.setAinntektsfilter("8-28");
        request.setFormaal("Foreldrepenger");

        request.setMaanedFom(finnInntektRequest.fom().isAfter(INNTK_TIDLIGSTE_DATO) ? finnInntektRequest.fom() : INNTK_TIDLIGSTE_DATO);
        request.setMaanedTom(finnInntektRequest.tom().isAfter(INNTK_TIDLIGSTE_DATO) ? finnInntektRequest.tom() : INNTK_TIDLIGSTE_DATO);
        return RestRequest.newPOSTJson(request, restConfig.endpoint(), restConfig);
    }

}
