package no.nav.foreldrepenger.inntektsmelding.integrasjoner.joark;

import jakarta.enterprise.context.Dependent;

import no.nav.vedtak.felles.integrasjon.dokarkiv.AbstractDokArkivKlient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "dokarkiv.base.url", endpointDefault = "http://dokarkiv.teamdokumenthandtering/rest/journalpostapi/v1/journalpost", scopesProperty = "dokarkiv.scopes", scopesDefault = "api://prod-fss.teamdokumenthandtering.dokarkiv/.default")
public class JoarkKlient extends AbstractDokArkivKlient {

    protected JoarkKlient() {
        super();
    }
}
