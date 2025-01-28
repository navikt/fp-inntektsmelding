package no.nav.familie.inntektsmelding.integrasjoner.person;

import jakarta.enterprise.context.Dependent;

import no.nav.vedtak.felles.integrasjon.person.AbstractPersonKlient;
import no.nav.vedtak.felles.integrasjon.person.Tema;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "pdl.base.url", endpointDefault = "https://pdl-api.prod-fss-pub.nais.io/graphql", scopesProperty = "pdl.scopes", scopesDefault = "api://prod-fss.pdl.pdl-api/.default")
@Dependent
class PdlKlient extends AbstractPersonKlient {

    PdlKlient() {
        super(RestClient.client(), Tema.FOR);
    }

    PdlKlient(Tema tema) {
        super(RestClient.client(), tema);
    }

}
