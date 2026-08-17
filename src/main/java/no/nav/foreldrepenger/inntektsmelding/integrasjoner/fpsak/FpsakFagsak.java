package no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak;

import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;

import java.time.LocalDate;

public record FpsakFagsak(StatusSakInntektsmelding statusInntektsmelding, LocalDate førsteUttaksdato, LocalDate skjæringstidspunkt, Saksnummer saksnummer){
    public enum StatusSakInntektsmelding {
        ÅPEN_FOR_BEHANDLING,
        SØKT_FOR_TIDLIG,
        VENTER_PÅ_SØKNAD,
        PAPIRSØKNAD_IKKE_REGISTRERT,
        INGEN_BEHANDLING
    }
}
