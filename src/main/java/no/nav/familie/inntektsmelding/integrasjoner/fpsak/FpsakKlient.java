package no.nav.familie.inntektsmelding.integrasjoner.fpsak;

import java.net.URI;

import jakarta.enterprise.context.Dependent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPSAK)
public class FpsakKlient {
    private static final Logger LOG = LoggerFactory.getLogger(FpsakKlient.class);

    private static final String FPSAK_API = "/api/fordel/sakInntektsmelding";

    private final RestClient restClient;
    private final RestConfig restConfig;

    public FpsakKlient() {
        this(RestClient.client());
    }

    FpsakKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public boolean harSøkerSakIFagsystem(AktørIdEntitet aktørIdEntitet, Ytelsetype ytelsetype) {
        var uri = uri();
        LOG.info("Undersøker om aktør {} har en sak på ytelse {}", aktørIdEntitet, ytelsetype);
        var ytelseDto = ytelsetype.equals(Ytelsetype.FORELDREPENGER) ? InntektsmeldingSakRequest.Ytelse.FORELDREPENGER : InntektsmeldingSakRequest.Ytelse.SVANGERSKAPSPENGER;
        var requestDto = new InntektsmeldingSakRequest(new InntektsmeldingSakRequest.AktørId(aktørIdEntitet.getAktørId()), ytelseDto);
        var request = RestRequest.newPOSTJson(requestDto, uri, restConfig);
        try {
            return restClient.sendReturnOptional(request, SakInntektsmeldingResponse.class).map(SakInntektsmeldingResponse::søkerHarSak)
                .orElseThrow(() -> new IllegalStateException("Klarte ikke spørre fpsak om søkers fagsaker"));
        } catch (Exception e) {
            LOG.warn("FP-INNTEKTSMELDING: Integrasjonsfeil mot fpsak. Klarte ikke sjekke om søker har saker. Fikk feil: {}. Returnerer default false", e.toString());
            return false;
        }
    }

    private URI uri() {
        return UriBuilder.fromUri(restConfig.endpoint()).path(FPSAK_API).build();
    }

    public record InntektsmeldingSakRequest(@Valid @NotNull AktørId bruker, @Valid @NotNull Ytelse ytelse){
        protected record AktørId(@NotNull @Digits(integer = 19, fraction = 0) String aktørId){}
        protected enum Ytelse{FORELDREPENGER, SVANGERSKAPSPENGER}
    }
    public record SakInntektsmeldingResponse(boolean søkerHarSak){}
}
