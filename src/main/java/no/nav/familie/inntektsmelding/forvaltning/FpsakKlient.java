package no.nav.familie.inntektsmelding.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.ws.rs.core.UriBuilder;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPSAK)
public class FpsakKlient {
    private static final Logger LOG = LoggerFactory.getLogger(FpsakKlient.class);

    private static final String FPSAK_API = "/api";

    private final RestClient restClient;
    private final RestConfig restConfig;

    FpsakKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public boolean erInntektsmeldingFortsattPåkrevd(ForespørselEntitet forespørselEntitet) {
        var uri = uri();
        var requestDto = new KontrollerSakForespørselRequest(forespørselEntitet.getFagsystemSaksnummer().orElseThrow(),
            forespørselEntitet.getOrganisasjonsnummer());
        LOG.info("Sender forespørsel request på saksnummer: {}", requestDto.saksnummer());
        var request = RestRequest.newPOSTJson(requestDto, uri, restConfig);

        return restClient.sendReturnOptional(request, KontrollerSakForespørselResponse.class).map(KontrollerSakForespørselResponse::erInntektsmeldingPåkrevd)
            .orElseThrow(() -> new IllegalStateException("Klarte kontrollere forespørsel: " + forespørselEntitet.getUuid()));
    }

    private URI uri() {
        return UriBuilder.fromUri(restConfig.endpoint()).path(FPSAK_API).path("/behandling/inntektsmelding/kontroller-forespoersel").build();
    }

    public record KontrollerSakForespørselRequest(String saksnummer, String orgnr){}
    public record KontrollerSakForespørselResponse(Boolean erInntektsmeldingPåkrevd){}
}
