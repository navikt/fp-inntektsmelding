package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.sikkerhet.oidc.token.impl.MaskinportenTokenKlient;
import no.nav.vedtak.util.LRUCache;

public class AltinnExchangeTokenKlient {

    private static final Logger LOG = LoggerFactory.getLogger(AltinnExchangeTokenKlient.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");

    private static final Environment ENV = Environment.current();

    private static AltinnExchangeTokenKlient instance;

    private final LRUCache<String, String> altinnCache;

    private AltinnExchangeTokenKlient() {
        this.altinnCache = new LRUCache<>(2, TimeUnit.MILLISECONDS.convert(29, TimeUnit.MINUTES));
    }

    public static synchronized AltinnExchangeTokenKlient instance() {
        var inst = instance;
        if (inst == null) {
            inst = new AltinnExchangeTokenKlient();
            instance = inst;
        }
        return inst;
    }

    public String hentAltinnToken(String scopes) {
        return veksleTilAltinn3Token(MaskinportenTokenKlient.instance().hentMaskinportenToken(scopes, null).token());
    }

    private String veksleTilAltinn3Token(String maskinportenToken) {
        var cacheKey = cacheKey(maskinportenToken);
        var tokenFromCache = getCachedToken(cacheKey);
        if (tokenFromCache != null) {
            LOG.debug("Fant altinn token i cache med nøkkel: {}", cacheKey);
            return tokenFromCache;
        }
        LOG.debug("Fant ingen gyldig altinn token i cache med nøkkel: {}", cacheKey);

        var exchangeRequest = HttpRequest.newBuilder()
            .header("Cache-Control", "no-cache")
            .header("Authorization", "Bearer " + maskinportenToken)
            .timeout(Duration.ofSeconds(3))
            .uri(URI.create(ENV.getRequiredProperty("altinn.tre.token.exchange.path")))
            .GET()
            .build();

        var token = AltinnExchangeTokenKlient.hentTokenRetryable(exchangeRequest, 3);
        SECURE_LOG.info("Altinn leverte dialogporten token: {}", token);
        putTokenToCache(cacheKey, token);
        LOG.debug("Hentet altinn token og lagt i cache med nøkkel: {}", cacheKey);
        return token;
    }

    private String getCachedToken(String key) {
        return altinnCache.get(key);
    }

    private void putTokenToCache(String key, String exchangedToken) {
        altinnCache.put(key, exchangedToken);
    }

    private String cacheKey(String maskinportenToken) {
        try {
            var md = MessageDigest.getInstance("MD5");
            byte[] keyBytes = maskinportenToken.getBytes();
            byte[] hash = md.digest(keyBytes);
            var hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            md.reset();
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new TekniskException("PKI-845346","MD5 algoritme finnes ikke", e);
        }
    }

    public static String hentTokenRetryable(HttpRequest request, int retries) {
        int i = retries;
        while (i-- > 0) {
            try {
                return hentToken(request);
            } catch (TekniskException e) {
                LOG.info("Feilet {}. gang ved henting av token. Prøver på nytt", retries - i, e);
            }
        }
        return hentToken(request);
    }

    private static String hentToken(HttpRequest request) {
        try (var client = hentEllerByggHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
            if (response == null || response.body() == null || !responskode2xx(response)) {
                throw new TekniskException("F-157385", "Kunne ikke hente token");
            }
            return response.body();
        } catch (IOException e) {
            throw new TekniskException("F-432937", "IOException ved kommunikasjon med server", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TekniskException("F-432938", "InterruptedException ved henting av token", e);
        }
    }

    private static boolean responskode2xx(HttpResponse<String> response) {
        var status = response.statusCode();
        return status >= 200 && status < 300;
    }

    private static HttpClient hentEllerByggHttpClient() {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(2))
            .proxy(HttpClient.Builder.NO_PROXY)
            .build();
    }

}
