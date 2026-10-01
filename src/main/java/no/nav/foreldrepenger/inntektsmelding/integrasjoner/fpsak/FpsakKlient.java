package no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.exception.IntegrasjonException;
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

    private static final String FPSAK_SAKSOVERSIKT = "/api/fordel/inntektsmeldingSaksoversikt";
    private static final String FPSAK_FORESPØRSEL_STATUS = "/api/fordel/inntektsmelding/forespoersel-status";

    private final RestClient restClient;
    private final RestConfig restConfig;

    public FpsakKlient() {
        this(RestClient.client());
    }

    FpsakKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public List<InfoOmSakInntektsmeldingResponse> hentSaksoversiktRelevantForInntektsmeldinger(AktørId aktørId, Ytelsetype ytelsetype) {
        var uri = UriBuilder.fromUri(restConfig.endpoint()).path(FPSAK_SAKSOVERSIKT).build();
        LOG.info("Henter saksoversikt for aktør {}", aktørId);
        var ytelseDto = ytelsetype.equals(Ytelsetype.FORELDREPENGER) ? InntektsmeldingSakRequest.Ytelse.FORELDREPENGER : InntektsmeldingSakRequest.Ytelse.SVANGERSKAPSPENGER;
        var requestDto = new InntektsmeldingSakRequest(new InntektsmeldingSakRequest.AktørId(aktørId.getAktørId()), ytelseDto);
        var request = RestRequest.newPOSTJson(requestDto, uri, restConfig);
        try {
            return restClient.sendReturnList(request, InfoOmSakInntektsmeldingResponse.class);
        } catch (Exception e) {
            throw new IntegrasjonException("FPINNTEKTSMELDING-694578", "Integrasjonsfeil mot fpsak. Klarte ikke hente saksoversikt. Fikk feil: " + e);
        }
    }

    /**
     * Sjekker mot fp-sak om det fortsatt er behov for inntektsmelding for en gitt (fagsakSaksnummer, orgnummer).
     * Brukes av forvaltningsjobber for å rydde opp forespørsler uten behov.
     */
    public boolean sjekkForespørselStatus(String fagsakSaksnummer, String orgnummer) {
        var uri = UriBuilder.fromUri(restConfig.endpoint()).path(FPSAK_FORESPØRSEL_STATUS).build();
        LOG.info("Sjekker forespørselstatus mot fp-sak for saksnummer={}, orgnummer={}", fagsakSaksnummer, orgnummer);
        var requestDto = new ForespørselStatusRequest(fagsakSaksnummer, orgnummer);
        var request = RestRequest.newPOSTJson(requestDto, uri, restConfig);
        try {
            return restClient.send(request, Boolean.class);
        } catch (Exception e) {
            throw new IntegrasjonException("FPINNTEKTSMELDING-694579",
                "Integrasjonsfeil mot fpsak. Klarte ikke sjekke forespørselstatus for saksnummer=" + fagsakSaksnummer
                    + ", orgnummer=" + orgnummer + ".", e);
        }
    }

    public record InntektsmeldingSakRequest(@Valid @NotNull AktørId bruker, @Valid @NotNull Ytelse ytelse){
        protected record AktørId(@NotNull @Digits(integer = 19, fraction = 0) String aktørId){}
        protected enum Ytelse{FORELDREPENGER, SVANGERSKAPSPENGER}
    }

    public record InfoOmSakInntektsmeldingResponse(@NotNull @Valid StatusSakInntektsmelding statusInntektsmelding, @NotNull LocalDate førsteUttaksdato, @NotNull LocalDate skjæringstidspunkt, @NotNull String saksnummer) {}
    public enum StatusSakInntektsmelding {
        ÅPEN_FOR_BEHANDLING,
        SØKT_FOR_TIDLIG,
        //På sikt vil ikke denne være relevant siden det ikke er mulig å sende inntektsmelding før søknad er mottatt (når altinn2 er skrudd av)
        VENTER_PÅ_SØKNAD,
        PAPIRSØKNAD_IKKE_REGISTRERT,
        INGEN_BEHANDLING
    }

    public record ForespørselStatusRequest(@NotNull String fagsakSaksnummer, @NotNull String orgnummer) {
    }
}
