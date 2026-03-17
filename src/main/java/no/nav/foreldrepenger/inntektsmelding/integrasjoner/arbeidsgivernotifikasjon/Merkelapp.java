package no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

public enum Merkelapp {
    INNTEKTSMELDING_FP("Inntektsmelding foreldrepenger"),
    INNTEKTSMELDING_SVP("Inntektsmelding svangerskapspenger")
    ;

    private final String beskrivelse;

    Merkelapp(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}


