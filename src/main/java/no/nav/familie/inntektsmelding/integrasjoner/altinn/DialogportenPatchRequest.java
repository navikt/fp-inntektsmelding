package no.nav.familie.inntektsmelding.integrasjoner.altinn;

public record DialogportenPatchRequest(String op, String path, Object value) {

    public static final String OP_ADD = "add";
    public static final String OP_REPLACE = "replace";
    public static final String OP_REMOVE = "remove";

    public static final String PATH_STATUS = "/status";
    public static final String PATH_EXTENDED_STATUS = "/extendedStatus";
    public static final String PATH_EXTERNAL_REFERENCE = "/externalReference";
    public static final String PATH_CONTENT = "/content";
    public static final String PATH_TRANSMISSIONS = "/transmissions";

}
