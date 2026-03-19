package no.nav.familie.inntektsmelding.imdialog.tjenester.ekstern;

import java.util.Optional;
import java.util.UUID;

public record EksternInntektsmeldingResponseDto(boolean success, Optional<UUID> inntektsmeldingUuid, String melding) {
}
