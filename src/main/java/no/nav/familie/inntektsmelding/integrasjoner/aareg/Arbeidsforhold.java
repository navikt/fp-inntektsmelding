package no.nav.familie.inntektsmelding.integrasjoner.aareg;

import java.time.LocalDate;

public record Arbeidsforhold(String organisasjonsnummer, Ansettelsesperiode ansettelsesperiode) {
    public record Ansettelsesperiode (LocalDate fom, LocalDate tom) {}
}
