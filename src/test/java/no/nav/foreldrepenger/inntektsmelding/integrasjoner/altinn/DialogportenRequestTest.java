package no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class DialogportenRequestTest {

    @Test
    void serdes_test() {
        var orgnr = "987654321";
        var party = "urn:altinn:organization:identifier-no:%s".formatted(orgnr);
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

        var serialized = DefaultJsonMapper.toJson(request);

        var deserialized = DefaultJsonMapper.fromJson(serialized, DialogportenRequest.class);

        assertThat(deserialized).isEqualTo(request);
    }
}
