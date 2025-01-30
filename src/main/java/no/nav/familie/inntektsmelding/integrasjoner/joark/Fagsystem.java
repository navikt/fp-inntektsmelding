package no.nav.familie.inntektsmelding.integrasjoner.joark;

public enum Fagsystem {

    FPSAK("FS36"),
    ;

    private String offisiellKode;

    Fagsystem() {
        // Hibernate trenger den
    }

    private Fagsystem(String offisiellKode) {
        this.offisiellKode = offisiellKode;
    }

    public String getOffisiellKode() {
        return offisiellKode;
    }
}
