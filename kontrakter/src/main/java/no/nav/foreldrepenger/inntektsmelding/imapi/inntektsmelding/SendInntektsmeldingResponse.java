package no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding;

import no.nav.foreldrepenger.inntektsmelding.felles.FeilkodeDto;
import no.nav.foreldrepenger.inntektsmelding.felles.InntektsmeldingStatusDto;

import java.util.UUID;

public record SendInntektsmeldingResponse(boolean success, UUID inntektsmeldingUuid, InntektsmeldingStatusDto status, FeilInfo feilinformasjon) {
    public record FeilInfo(FeilkodeDto feilkode, String feilmelding, String referanseId) {}
}
