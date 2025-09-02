package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class DialogportenRequestTest {

    private ObjectMapper OBJECT_MAPPER = DefaultJsonMapper.getObjectMapper();

    @Test
    void serdes_test() throws JsonProcessingException {
        var orgnr = "987654321";
        var party = String.format("urn:altinn:organization:identifier-no:%s", orgnr);
        var serviceResource = "urn:altinn:resource:nav_foreldrepenger_inntektsmelding";
        var externalReference = "saksnummer-123456789";

        DialogportenRequest request = new DialogportenRequest(serviceResource,
            party,
            externalReference,
            DialogportenRequest.DialogStatus.InProgress,
            new DialogportenRequest.Content(
                new DialogportenRequest.ContentValue(List.of(new DialogportenRequest.ContentValueItem("Inntektsmelding", DialogportenRequest.NB)), DialogportenRequest.TEXT_PLAIN),
                new DialogportenRequest.ContentValue(List.of(new DialogportenRequest.ContentValueItem("Sammendrag", DialogportenRequest.NB)), DialogportenRequest.TEXT_PLAIN),null),
            null,
            null);

        var serialized = OBJECT_MAPPER.writeValueAsString(request);

        var deserialized = OBJECT_MAPPER.readValue(serialized, DialogportenRequest.class);

        assertThat(deserialized).isEqualTo(request);
    }
}
