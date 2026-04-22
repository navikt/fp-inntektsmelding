package no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding;

import no.nav.foreldrepenger.inntektsmelding.felles.FeilkodeDto;

import java.util.UUID;

public record SendInntektsmeldingResponse(boolean success, UUID inntektsmeldingUuid, FeilInfo feilinformasjon) {
    public record FeilInfo(FeilkodeDto feilkode, String feilmelding, String feilreferanse) {} ;
}
