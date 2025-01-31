package no.nav.familie.inntektsmelding.integrasjoner.person;

import java.time.LocalDate;

import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

public record PersonInfo(String fornavn, String mellomnavn, String etternavn, PersonIdent fødselsnummer, AktørIdEntitet aktørId,
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
