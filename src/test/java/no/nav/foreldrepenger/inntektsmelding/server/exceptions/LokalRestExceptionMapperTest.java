package no.nav.foreldrepenger.inntektsmelding.server.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import ch.qos.logback.classic.Level;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.feil.FeilDto;
import no.nav.vedtak.feil.Feilkode;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.log.util.MemoryAppender;
import no.nav.vedtak.server.rest.FeilUtils;

@Execution(ExecutionMode.SAME_THREAD)
class LokalRestExceptionMapperTest {

    private static MemoryAppender logSniffer;

    private final LokalRestExceptionMapper exceptionMapper = new LokalRestExceptionMapper();

    @BeforeEach
    void setUp() {
        logSniffer = MemoryAppender.sniff(FeilUtils.class);
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

            assertThat(feilDto.type()).isEqualTo(Feilkode.IKKE_TILGANG.name());
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

            assertThat(feilDto.type()).isEqualTo(InntektsmeldingException.LokalFeilKode.INGEN_SAK_FUNNET.name());
            assertThat(feilDto.callId()).isEqualTo(callId);
            assertThat(feilDto.feilmelding()).isEqualTo("Ingen sak funnet");
            assertThat(logSniffer.search(InntektsmeldingException.LokalFeilKode.INGEN_SAK_FUNNET.getFeiltekst(), Level.INFO)).hasSize(1);
        }
    }

    @Test
    void skalMappeFunksjonellFeilSendtForTidlig() {
        var callId = MDCOperations.generateCallId();
        MDCOperations.putCallId(callId);
        try (var response = exceptionMapper.toResponse(funksjonellFeilSendtForTidlig())) {
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
            var feilDto = (FeilDto) response.getEntity();

            assertThat(feilDto.type()).isEqualTo(InntektsmeldingException.LokalFeilKode.SENDT_FOR_TIDLIG.name());
            assertThat(feilDto.callId()).isEqualTo(callId);
            assertThat(feilDto.feilmelding()).isEqualTo("Sendt inntektsmelding for tidlig");
            assertThat(logSniffer.search(InntektsmeldingException.LokalFeilKode.SENDT_FOR_TIDLIG.getFeiltekst(), Level.INFO)).hasSize(1);
        }
    }

    @Test
    void skalMappeFunksjonellFeilOrgnrFinnesIAaReg() {
        var callId = MDCOperations.generateCallId();
        MDCOperations.putCallId(callId);
        try (var response = exceptionMapper.toResponse(funksjonellFeilFinnesIAaReg())) {
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
            var feilDto = (FeilDto) response.getEntity();

            assertThat(feilDto.type()).isEqualTo(InntektsmeldingException.LokalFeilKode.FINNES_I_AAREG.name());
            assertThat(feilDto.callId()).isEqualTo(callId);
            assertThat(feilDto.feilmelding()).isEqualTo("Organisasjonsnummer er rapportert i Aa-reg");
            assertThat(logSniffer.search(InntektsmeldingException.LokalFeilKode.FINNES_I_AAREG.getFeiltekst(), Level.INFO)).hasSize(1);
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
        return new InntektsmeldingException(InntektsmeldingException.LokalFeilKode.INGEN_SAK_FUNNET);
    }

    private static FunksjonellException funksjonellFeilSendtForTidlig() {
        return new InntektsmeldingException(InntektsmeldingException.LokalFeilKode.SENDT_FOR_TIDLIG);
    }
    private static FunksjonellException funksjonellFeilFinnesIAaReg() {
        return new InntektsmeldingException(InntektsmeldingException.LokalFeilKode.FINNES_I_AAREG);
    }

}
