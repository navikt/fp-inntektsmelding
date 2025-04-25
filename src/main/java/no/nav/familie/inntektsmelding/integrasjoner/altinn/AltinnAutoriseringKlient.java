package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.rest.NavHeaders;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, endpointProperty = "altinn.url", scopesProperty = "altinn.scopes")
public class AltinnAutoriseringKlient {
    private static final String SERVICE_CODE = "4936";
    private static final String SERVICE_EDITION = "1";
    private static final String FILTER_AKTIVE_BEDRIFTER = "Type ne 'Person' and Status eq 'Active'";
    /**
     * Altinn takler ikke høyere limit
     */
    static final int ALTINN_SIZE_LIMIT = 500;
    /**
     * Antar at ingen har tilganger til flere enn dette, for å unngå uendelig antall kall ved feil
     */
    private static final int ALTINN_TOTAL_SIZE_LIMIT = 100_000;

    private static AltinnAutoriseringKlient instance = new AltinnAutoriseringKlient();

    private final RestClient restClient;
    private final RestConfig restConfig;


    private AltinnAutoriseringKlient() {
        this(RestClient.client());
    }

    AltinnAutoriseringKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public static synchronized AltinnAutoriseringKlient instance() {
        var inst = instance;
        if (inst == null) {
            inst = new AltinnAutoriseringKlient();
            instance = inst;
        }
        return inst;
    }

    public boolean harTilgangTilBedriften(String orgnr) {
        return gjørKallMedPagineringOgRetry().stream().anyMatch(reportee -> orgnr.equals(reportee.organizationNumber()));
    }

    private List<AltinnReportee> gjørKallMedPagineringOgRetry() {
        List<AltinnReportee> altinnReportees = new ArrayList<>();

        int skip = 0;
        while (skip < ALTINN_TOTAL_SIZE_LIMIT) {
            var respons = gjørKall(skip);
            altinnReportees.addAll(respons);

            if (respons.size() < ALTINN_SIZE_LIMIT) {
                break;
            } else {
                skip += ALTINN_SIZE_LIMIT;
            }
        }

        return altinnReportees;
    }

    private List<AltinnReportee> gjørKall(int skip) {
        var uri = UriBuilder.fromUri(restConfig.endpoint())
            .queryParam("ForceEIAuthentication", "")
            .queryParam("serviceCode", SERVICE_CODE)
            .queryParam("serviceEdition", SERVICE_EDITION)
            .queryParam("$filter", FILTER_AKTIVE_BEDRIFTER)
            .queryParam("$top", ALTINN_SIZE_LIMIT)
            .queryParam("$skip", skip)
            .build();
        var request = RestRequest.newGET(uri, restConfig);
        request.otherCallId(NavHeaders.HEADER_NAV_CORRELATION_ID);
        try {
            return restClient.sendReturnList(request, AltinnReportee.class);
        } catch (RuntimeException e) {
            throw new IntegrasjonException("FP-965432",
                "Feil ved kall til altinn-rettigheter-proxy. Meld til #team_fager hvis dette skjer over lengre tidsperiode.", e);
        }
    }

    public List<String> hentBedrifterArbeidsgiverHarTilgangTil() {
        return gjørKallMedPagineringOgRetry().stream()
            .map(AltinnReportee::organizationNumber)
            .toList();
    }

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record AltinnReportee(String name,
                          String organizationForm,
                          String organizationNumber,
                          String parentOrganizationNumber,
                          String socialSecurityNumber,
                          String status,
                          String type) {
    }
}
