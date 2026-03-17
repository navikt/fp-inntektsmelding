package no.nav.foreldrepenger.inntektsmelding.integrasjoner.joark;

enum Behandlingtema {
    FORELDREPENGER("ab0326"),
    SVANGERSKAPSPENGER("ab0126")
    ;

    private final String offisiellKode;

    Behandlingtema(String offisiellKode) {
        this.offisiellKode = offisiellKode;
    }

    public String getOffisiellKode() {
        return offisiellKode;
    }
}
