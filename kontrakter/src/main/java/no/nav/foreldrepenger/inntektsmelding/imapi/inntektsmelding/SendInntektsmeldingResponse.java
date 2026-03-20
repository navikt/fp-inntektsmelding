package no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt;

import java.util.Optional;
import java.util.UUID;

public record EksternInntektsmeldingResponseDto(boolean success, Optional<UUID> inntektsmeldingUuid, String melding) {
}
