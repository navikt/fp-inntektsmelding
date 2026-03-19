package no.nav.foreldrepenger.inntektsmelding.integrasjoner.aareg.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpplysningspliktigArbeidsgiverDto(Type type, String organisasjonsnummer, String aktoerId, String offentligIdent) {

    @Override
    public String toString() {
        return "OpplysningspliktigArbeidsgiverDto{" + "type=" + type + ", organisasjonsnummer='" + organisasjonsnummer + '\'' + ", aktoerId='"
            + aktoerId + '\'' + ", offentligIdent='" + offentligIdent + '\'' + '}';
    }

    public enum Type {
        @JsonProperty("Organisasjon")
        ORGANISASJON,
        @JsonProperty("Person")
        PERSON
    }
}
