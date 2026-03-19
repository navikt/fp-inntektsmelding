package no.nav.foreldrepenger.inntektsmelding.integrasjoner.person;

import java.time.LocalDate;

public record PersonInfo(String fornavn, String mellomnavn, String etternavn, PersonIdent fødselsnummer, AktørId aktørId,
                         LocalDate fødselsdato, String telefonnummer, Kjønn kjønn) {

    public String mapNavn() {
        if (etternavn == null || fornavn == null) {
            return "";
        }
        return fornavn + (mellomnavn == null ? "" : " " + mellomnavn) + " " + etternavn;
    }

    public String mapFulltNavn() {
        if (etternavn == null || fornavn == null) {
            return "";
        }
        return fornavn + (mellomnavn == null ? "" : " " + mellomnavn) +" "+ etternavn;
    }

    public String mapFornavn() {
        if (fornavn == null) {
            return "";
        }
        return fornavn;
    }

    public enum Kjønn {
        MANN,
        KVINNE,
        UKJENT
    }
}
