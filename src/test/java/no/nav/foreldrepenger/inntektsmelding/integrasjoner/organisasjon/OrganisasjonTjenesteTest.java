package no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.vedtak.felles.integrasjon.organisasjon.OrganisasjonEReg;

@ExtendWith(MockitoExtension.class)
class OrganisasjonTjenesteTest {
    private OrganisasjonTjeneste organisasjonTjeneste;

    @Mock
    private EregKlient eregRestKlient;

    @Mock
    private OrganisasjonEReg respons;

    @BeforeEach
    void setUp() {
        organisasjonTjeneste = new OrganisasjonTjeneste(eregRestKlient);
    }

    @Test
    void respons_blir_mappet() {
        var testOrgnr = "999999999";
        var testNavn = "Testbedrift";
        when(eregRestKlient.hentOrganisasjon(testOrgnr)).thenReturn(respons);
        when(respons.getNavn()).thenReturn(testNavn);
        when(respons.organisasjonsnummer()).thenReturn(testOrgnr);
        var organisasjon = organisasjonTjeneste.finnOrganisasjonOptional(testOrgnr).orElseThrow();

        assertThat(organisasjon.navn()).isEqualTo(testNavn);
        assertThat(organisasjon.orgnr()).isEqualTo(testOrgnr);
    }
}
