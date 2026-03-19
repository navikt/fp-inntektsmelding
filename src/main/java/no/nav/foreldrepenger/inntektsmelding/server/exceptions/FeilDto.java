package no.nav.foreldrepenger.inntektsmelding.server.exceptions;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.inntektsmelding.server.exceptions.FeilType.GENERELL_FEIL;

import java.util.Collection;

public record FeilDto(FeilType type, String feilmelding, Collection<FeltFeilDto> feltFeil, String callId) {

    public FeilDto(FeilType type, String feilmelding, String callId) {
        this(type, feilmelding, emptyList(), callId);
    }

    public FeilDto(String feilmelding, Collection<FeltFeilDto> feltFeil, String callId) {
        this(GENERELL_FEIL, feilmelding, feltFeil, callId);
    }

    public FeilDto(String feilmelding) {
        this(GENERELL_FEIL, feilmelding, emptyList(), null);
    }

}
