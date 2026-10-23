package no.nav.foreldrepenger.inntektsmelding.integrasjoner.aareg;

import java.time.LocalDate;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ArbeidsforholdType;

public record Arbeidsforhold(String organisasjonsnummer, Ansettelsesperiode ansettelsesperiode, ArbeidsforholdType type) {
    public record Ansettelsesperiode (LocalDate fom, LocalDate tom) {}
}
