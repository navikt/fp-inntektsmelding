package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.familie.inntektsmelding.server.jackson.JacksonJsonConfig;

class AltinnReporteeTest {
    @Test
    void mapping_til_AltinnReportee() throws JsonProcessingException {
        String jsonAsString = """
            {
            "Name":"something",
            "OrganizationForm":"something",
            "OrganizationNumber":"something",
            "ParentOrganizationNumber":"something",
            "SocialSecurityNumber":"something",
            "Status":"something",
            "Type":"something",
            "Unknown":"property"
            }
            """;
        // Bruker samme ObjectMapper som resten av applikasjonen.
        // Merk at getContext(Object.class) ikke bryr seg om hvilken klasse som oppgis som parameter.
        var objectMapper = new JacksonJsonConfig().getContext(Object.class);
        AltinnRettigheterProxyKlient.AltinnReportee readValue = objectMapper.readValue(jsonAsString, AltinnRettigheterProxyKlient.AltinnReportee.class);

        assertThat(readValue).hasNoNullFieldsOrProperties();
    }
}
