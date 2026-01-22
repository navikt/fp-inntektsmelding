package no.nav.familie.inntektsmelding.integrasjoner.aareg;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.ArbeidsforholdDto;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.rest.NavHeaders;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

/*
 * Dokumentasjon https://confluence.adeo.no/display/FEL/AAREG+-+Tjeneste+REST+aareg.api
 * Swagger https://aareg-services-q2.dev.intern.nav.no/swagger-ui/index.html?urls.primaryName=aareg.api.v1#/arbeidstaker/finnArbeidsforholdPrArbeidstaker
 * Swagger V2 https://aareg-services-q2.dev.intern.nav.no/swagger-ui/index.html?urls.primaryName=aareg.api.v2#/arbeidstaker/finnArbeidsforholdPrArbeidstaker
 */

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "aareg.rs.url",
    endpointDefault = "https://aareg-services.dev-fss-pub.nais.io",
    scopesProperty = "aareg.scopes", scopesDefault = "api://dev-fss.arbeidsforhold.aareg-services-nais/.default")
public class AaregRestKlient {

    private final RestClient restClient; // Setter på consumer-token fra STS
    private final RestConfig restConfig;

    public AaregRestKlient() {
        this(RestClient.client());
    }

    public AaregRestKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public List<ArbeidsforholdDto> finnArbeidsforholdForArbeidstaker(String personIdent, LocalDate førsteFraværsdag) {
        try {
            var uri = lagUriForForFinnArbeidsforholdForArbeidstaker(førsteFraværsdag, førsteFraværsdag);
            var request = RestRequest.newGET(uri, restConfig).header(NavHeaders.HEADER_NAV_PERSONIDENT, personIdent);
            var response = restClient.sendReturnUnhandled(request);

            if (response.statusCode() == Response.Status.NOT_FOUND.getStatusCode()) {
                // 404 betyr at det ikke finnes arbeidsforhold for personen, eller at personen ikke finnes
                return Collections.emptyList();
            }

            if (response.statusCode() >= 400) {
                throw new IntegrasjonException("FP-12345", "Feil ved henting av arbeidsforhold for person: " + response.body());
            }

            var arbeidsforhold = DefaultJsonMapper.fromJson(response.body(), ArbeidsforholdDto[].class);
            return Arrays.asList(arbeidsforhold);
        } catch (IntegrasjonException e) {
            if (e.getMessage().contains("404")) {
                return Collections.emptyList();
            }
            throw e;
        } catch (UriBuilderException | IllegalArgumentException _) {
            throw new IllegalArgumentException("Utviklerfeil syntax-exception for finnArbeidsforholdForArbeidstaker");
        }
    }

    /**
     * Kun eksponert for å kunne teste URI-bygging – skal ikke brukes ellers
     */
    URI lagUriForForFinnArbeidsforholdForArbeidstaker(LocalDate qfom, LocalDate qtom) {
        return UriBuilder.fromUri(restConfig.endpoint())
            .path("arbeidsforhold")
            .queryParam("ansettelsesperiodeFom", String.valueOf(qfom))
            .queryParam("ansettelsesperiodeTom", String.valueOf(qtom))
            .queryParam("regelverk", "A_ORDNINGEN")
            .queryParam("historikk", "true")
            .queryParam("sporingsinformasjon", "false")
            .build();
    }

}
