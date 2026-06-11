package no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding;

import no.nav.foreldrepenger.inntektsmelding.felles.FeilkodeDto;

import java.util.UUID;

// TODO: Legg til InntektsmeldingStatusDto status som parameter nr 3:
//       (boolean success, UUID inntektsmeldingUuid, InntektsmeldingStatusDto status, FeilInfo feilinformasjon)
public record SendInntektsmeldingResponse(boolean success, UUID inntektsmeldingUuid, FeilInfo feilinformasjon) {
    public record FeilInfo(FeilkodeDto feilkode, String feilmelding, String referanseId) {}
}
