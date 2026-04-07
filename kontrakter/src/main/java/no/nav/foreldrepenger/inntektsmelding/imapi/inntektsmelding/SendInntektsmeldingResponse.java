package no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding;

import java.util.UUID;

public record SendInntektsmeldingResponse(boolean success, UUID inntektsmeldingUuid, String melding) {
}
