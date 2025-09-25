package no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "inntekt.url",
    endpointDefault = "http://ikomp.prod-fss-pub.nais.io/rest/v2/inntekt",
    scopesProperty = "inntekt.scopes", scopesDefault = "api://prod-fss.team-inntekt.ikomp/.default")
public class InntektskomponentV2Klient {
    private static final Logger LOG = LoggerFactory.getLogger(InntektskomponentV2Klient.class);
    private static final YearMonth INNTK_TIDLIGSTE_DATO = YearMonth.of(2015, 7);

    private final RestClient restClient;
    private final RestConfig restConfig;

    public InntektskomponentV2Klient() {
        this(RestClient.client());
    }

    public InntektskomponentV2Klient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public List<Inntektsinformasjon> finnInntekt(FinnInntektRequest finnInntektRequest) {
        var request = lagRequest(finnInntektRequest);
        LOG.info("Henter inntekt");

        try {
            return restClient.sendReturnOptional(request, InntektApiUt.class)
                .map(InntektApiUt::data).orElseGet(List::of);
        } catch (RuntimeException e) {
            throw new IntegrasjonException("FP-824246",
                "Feil ved kall til inntektstjenesten. Meld til #team_registre og #produksjonshendelser hvis dette skjer over lengre tidsperiode.", e);
        }
    }

    private RestRequest lagRequest(FinnInntektRequest finnInntektRequest) {
        var request = new InntektskomponentV2Klient.InntektApiInn(finnInntektRequest.aktørId(), "8-28", "Foreldrepenger",
            finnInntektRequest.fom().isAfter(INNTK_TIDLIGSTE_DATO) ? finnInntektRequest.fom() : INNTK_TIDLIGSTE_DATO,
            finnInntektRequest.tom().isAfter(INNTK_TIDLIGSTE_DATO) ? finnInntektRequest.tom() : INNTK_TIDLIGSTE_DATO);

        return RestRequest.newPOSTJson(request, restConfig.endpoint(), restConfig);
    }

    public record InntektApiInn(String personident, String filter, String formaal, YearMonth maanedFom, YearMonth maanedTom) { }

    public record InntektApiUt(List<Inntektsinformasjon> data) { }

    public record Inntektsinformasjon(YearMonth maaned, String underenhet, List<Inntekt> inntektListe) { }

    public record Inntekt(String type, BigDecimal beloep) { }

}
