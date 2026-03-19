package no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import javax.net.ssl.SSLSession;

import org.eclipse.jetty.http.HttpStatus;

import no.nav.vedtak.mapper.json.DefaultJsonMapper;

public class MockGraphQLHttpResponse<T> implements HttpResponse<String> {
    private final T body;

    public MockGraphQLHttpResponse(T body) {
        this.body = body;
    }

    @Override
    public int statusCode() {
        return HttpStatus.OK_200;
    }

    @Override
    public HttpRequest request() {
        return null;
    }

    @Override
    public Optional<HttpResponse<String>> previousResponse() {
        return Optional.empty();
    }

    @Override
    public HttpHeaders headers() {
        return null;
    }

    @Override
    public String body() {
        return DefaultJsonMapper.toJson(body);
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return Optional.empty();
    }

    @Override
    public URI uri() {
        return null;
    }

    @Override
    public HttpClient.Version version() {
        return null;
    }
}
