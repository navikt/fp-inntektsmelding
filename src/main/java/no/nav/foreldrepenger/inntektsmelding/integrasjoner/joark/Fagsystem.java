package no.nav.foreldrepenger.inntektsmelding.integrasjoner.joark;

enum Fagsystem {
    FPSAK("FS36")
    ;

    private String offisiellKode;

    Fagsystem(String offisiellKode) {
        this.offisiellKode = offisiellKode;
    }

    public String getOffisiellKode() {
        return offisiellKode;
    }
}
