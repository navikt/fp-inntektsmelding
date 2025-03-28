package no.nav.familie.inntektsmelding.server.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import no.nav.vedtak.log.mdc.MDCOperations;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import ch.qos.logback.classic.Level;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.log.util.MemoryAppender;

@Execution(ExecutionMode.SAME_THREAD)
class GeneralRestExceptionMapperTest {

    private static MemoryAppender logSniffer;

    private final GeneralRestExceptionMapper exceptionMapper = new GeneralRestExceptionMapper();

    @BeforeEach
    void setUp() {
        logSniffer = MemoryAppender.sniff(GeneralRestExceptionMapper.class);
    }

    @AfterEach
    void afterEach() {
        logSniffer.reset();
    }

    @Test
    void skalIkkeMappeManglerTilgangFeil() {
        var callId = MDCOperations.generateCallId();
        MDCOperations.putCallId(callId);

        try (var response = exceptionMapper.toResponse(manglerTilgangFeil())) {
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
            var feilDto = (FeilDto) response.getEntity();

            assertThat(feilDto.type()).isEqualTo(FeilType.MANGLER_TILGANG_FEIL);
            assertThat(feilDto.callId()).isEqualTo(callId);
            assertThat(feilDto.feilmelding()).isEqualTo("Mangler tilgang");
            assertThat(logSniffer.search("ManglerTilgangFeilmeldingKode", Level.WARN)).isEmpty();
        }
    }

    @Test
    void skalMappeFunksjonellFeil() {
        var callId = MDCOperations.generateCallId();
        MDCOperations.putCallId(callId);
        try (var response = exceptionMapper.toResponse(funksjonellFeil())) {
            assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
            var feilDto = (FeilDto) response.getEntity();

            assertThat(feilDto.feilmelding()).isEqualTo("Serverfeil");
            assertThat(feilDto.callId()).isEqualTo(callId);
            assertThat(logSniffer.search("en funksjonell feilmelding", Level.WARN)).hasSize(1);
        }
    }

    @Test
    void skalMappeFunksjonellFeilSakIkkeFunnet() {
        var callId = MDCOperations.generateCallId();
        MDCOperations.putCallId(callId);
        try (var response = exceptionMapper.toResponse(funksjonellFeilSakIkkeFunnet())) {
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
            var feilDto = (FeilDto) response.getEntity();

            assertThat(feilDto.type()).isEqualTo(FeilType.INGEN_SAK_FUNNET);
            assertThat(feilDto.callId()).isEqualTo(callId);
            assertThat(feilDto.feilmelding()).isEqualTo("Ingen sak funnet");
            assertThat(logSniffer.search("Ingen sak funnet feil", Level.INFO)).hasSize(1);
        }
    }

    @Test
    void skalMappeVLException() {
        var callId = MDCOperations.generateCallId();
        MDCOperations.putCallId(callId);
        try (var response = exceptionMapper.toResponse(tekniskFeil())) {
            assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
            var feilDto = (FeilDto) response.getEntity();

            assertThat(feilDto.callId()).isEqualTo(callId);
            assertThat(feilDto.feilmelding()).isEqualTo("Serverfeil");
            assertThat(logSniffer.search("en teknisk feilmelding", Level.WARN)).hasSize(1);
        }
    }

    @Test
    void skalMappeWrappedGenerellFeil() {
        var callId = MDCOperations.generateCallId();
        MDCOperations.putCallId(callId);
        var feilmelding = "en helt generell feil";
        var generellFeil = new RuntimeException(feilmelding);

        try (var response = exceptionMapper.toResponse(new TekniskException("KODE", "TEKST", generellFeil))) {
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
            var feilDto = (FeilDto) response.getEntity();

            assertThat(feilDto.callId()).isEqualTo(callId);
            assertThat(feilDto.feilmelding()).isEqualTo("Serverfeil");
            assertThat(logSniffer.search("TEKST", Level.WARN)).hasSize(1);
        }
    }

    @Test
    void skalMappeWrappedFeilUtenCause() {
        var callId = MDCOperations.generateCallId();
        MDCOperations.putCallId(callId);
        var feilmelding = "en helt generell feil";
        try (var response = exceptionMapper.toResponse(new TekniskException("KODE", feilmelding))) {
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
            var feilDto = (FeilDto) response.getEntity();

            assertThat(feilDto.callId()).isEqualTo(callId);
            assertThat(feilDto.feilmelding()).isEqualTo("Serverfeil");
            assertThat(logSniffer.search(feilmelding, Level.WARN)).hasSize(1);
        }
    }

    @Test
    void skalMappeGenerellFeil() {
        var callId = MDCOperations.generateCallId();
        MDCOperations.putCallId(callId);
        var feilmelding = "en helt generell feil";
        RuntimeException generellFeil = new IllegalArgumentException(feilmelding);

        try (var response = exceptionMapper.toResponse(generellFeil)) {
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
            var feilDto = (FeilDto) response.getEntity();

            assertThat(feilDto.callId()).isEqualTo(callId);
            assertThat(feilDto.feilmelding()).isEqualTo("Serverfeil");
            assertThat(logSniffer.search(feilmelding, Level.WARN)).hasSize(1);
        }
    }

    private static FunksjonellException funksjonellFeil() {
        return new FunksjonellException("FUNK_FEIL", "en funksjonell feilmelding", "et løsningsforslag");
    }

    private static TekniskException tekniskFeil() {
        return new TekniskException("TEK_FEIL", "en teknisk feilmelding");
    }

    private static ManglerTilgangException manglerTilgangFeil() {
        return new ManglerTilgangException("MANGLER_TILGANG_FEIL", "ManglerTilgangFeilmeldingKode");
    }

    private static FunksjonellException funksjonellFeilSakIkkeFunnet() {
        return new FunksjonellException("INGEN_SAK_FUNNET", "en egen funksjonell melding", null);
    }

}
