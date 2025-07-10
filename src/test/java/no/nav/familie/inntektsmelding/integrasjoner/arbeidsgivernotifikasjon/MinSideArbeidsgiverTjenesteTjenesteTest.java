package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import static no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjenesteImpl.SERVICE_CODE;
import static no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjenesteImpl.SERVICE_EDITION_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MinSideArbeidsgiverTjenesteTjenesteTest {

    @Mock
    MinSideArbeidsgiverKlient klient;

    private MinSideArbeidsgiverTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new MinSideArbeidsgiverTjenesteImpl(klient);
    }

    @Test
    void opprett_sak() {

        var expectedGrupperingsid = "id-som-knytter-sak-til-notifikasjon";
        var expectedVirksomhetsnummer = "2342342334";
        var expectedTittel = "Inntektsmelding for person";
        var expectedLenke = "https://inntektsmelding-innsendings-dialog.com";
        var expectedMerkelapp = Merkelapp.INNTEKTSMELDING_FP;

        var requestCaptor = ArgumentCaptor.forClass(NySakMutationRequest.class);

        tjeneste.opprettSak(expectedGrupperingsid, expectedMerkelapp, expectedVirksomhetsnummer, expectedTittel, URI.create(expectedLenke));

        Mockito.verify(klient).opprettSak(requestCaptor.capture(), any(NySakResultatResponseProjection.class));

        var request = requestCaptor.getValue();

        var input = request.getInput();
        assertThat(input).containsOnlyKeys("grupperingsid",
            "initiellStatus",
            "lenke",
            "merkelapp",
            "tittel",
            "virksomhetsnummer",
            "mottakere",
            "overstyrStatustekstMed",
            "nesteSteg",
            "tidspunkt",
            "tilleggsinformasjon",
            "hardDelete")
            .containsEntry("grupperingsid", expectedGrupperingsid)
            .containsEntry("initiellStatus", SaksStatus.UNDER_BEHANDLING)
            .containsEntry("lenke", expectedLenke)
            .containsEntry("merkelapp", expectedMerkelapp.getBeskrivelse())
            .containsEntry("tittel", expectedTittel)
            .containsEntry("virksomhetsnummer", expectedVirksomhetsnummer)
            .containsEntry("overstyrStatustekstMed", "");
        assertThat(input.get("mottakere")).isNotNull();
    }

    @Test
    void opprett_oppgave() {

        var expectedEksternId = "TestId";
        var expectedGrupperingsid = "id-som-knytter-sak-til-notifikasjon";
        var expectedVirksomhetsnummer = "2342342334";
        var expectedNotifikasjonsTekst = "Du har en ny oppgave i AG-portalen";
        var expectedEksternvarselTekst = "En ansatt har søkt foreldrepenger";
        var expectedPåminnelseTekst = "Påmminnelse: En ansatt har søkt foreldrepenger";
        var expectedNotifikasjonsLenke = "https://arbeidsgiver-portal.com";
        var expectedNotifikasjonsMerkelapp = Merkelapp.INNTEKTSMELDING_FP;

        var requestCaptor = ArgumentCaptor.forClass(NyOppgaveMutationRequest.class);

        tjeneste.opprettOppgave(expectedGrupperingsid,
            expectedNotifikasjonsMerkelapp,
            expectedEksternId,
            expectedVirksomhetsnummer,
            expectedNotifikasjonsTekst,
            expectedEksternvarselTekst,
            expectedPåminnelseTekst,
            URI.create(expectedNotifikasjonsLenke));

        Mockito.verify(klient).opprettOppgave(requestCaptor.capture(), any(NyOppgaveResultatResponseProjection.class));

        var input = requestCaptor.getValue().getInput();

        assertThat(input).isNotNull().hasSize(1);
        var inputKey = "nyOppgave";
        assertThat(input).containsKey(inputKey);
        assertThat(input.get(inputKey)).isInstanceOf(NyOppgaveInput.class);
        var nyOppgave = (NyOppgaveInput) input.get(inputKey);

        assertThat(nyOppgave.getMottaker()).isNotNull();
        assertThat(nyOppgave.getMottaker().getAltinn().getServiceCode()).isEqualTo(SERVICE_CODE);
        assertThat(nyOppgave.getMottaker().getAltinn().getServiceEdition()).isEqualTo(SERVICE_EDITION_CODE);

        assertThat(nyOppgave.getMetadata()).isNotNull();
        assertThat(nyOppgave.getMetadata().getEksternId()).isNotNull().isEqualTo(expectedEksternId);
        assertThat(nyOppgave.getMetadata().getGrupperingsid()).isNotNull().isEqualTo(expectedGrupperingsid);
        assertThat(nyOppgave.getMetadata().getVirksomhetsnummer()).isNotNull().isEqualTo(expectedVirksomhetsnummer);
        assertThat(nyOppgave.getMetadata().getOpprettetTidspunkt()).isNull();

        assertThat(nyOppgave.getNotifikasjon()).isNotNull();
        assertThat(nyOppgave.getNotifikasjon().getTekst()).isEqualTo(expectedNotifikasjonsTekst);
        assertThat(nyOppgave.getNotifikasjon().getLenke()).isEqualTo(expectedNotifikasjonsLenke);
        assertThat(nyOppgave.getNotifikasjon().getMerkelapp()).isEqualTo(expectedNotifikasjonsMerkelapp.getBeskrivelse());

        assertThat(nyOppgave.getEksterneVarsler()).hasSize(1);
        assertThat(nyOppgave.getEksterneVarsler().getFirst().getAltinntjeneste()).isNotNull();
        assertThat(nyOppgave.getEksterneVarsler().getFirst().getAltinntjeneste().getInnhold()).isEqualTo(expectedEksternvarselTekst);

        assertThat(nyOppgave.getPaaminnelse()).isNotNull();
        assertThat(nyOppgave.getPaaminnelse().getEksterneVarsler()).isNotNull().hasSize(1);
        assertThat(nyOppgave.getPaaminnelse().getEksterneVarsler().getFirst().getAltinntjeneste()).isNotNull();
        assertThat(nyOppgave.getPaaminnelse().getEksterneVarsler().getFirst().getAltinntjeneste().getInnhold()).isEqualTo(expectedPåminnelseTekst);

        assertThat(nyOppgave.getFrist()).isNull();
        assertThat(nyOppgave.getMottakere()).isEmpty();
    }

    @Test
    void lukk_oppgave() {
        var expectedId = "TestId";
        var expectedTidspunkt = OffsetDateTime.now();

        var requestCaptor = ArgumentCaptor.forClass(OppgaveUtfoertMutationRequest.class);

        tjeneste.oppgaveUtført(expectedId, expectedTidspunkt);

        Mockito.verify(klient).oppgaveUtført(requestCaptor.capture(), any(OppgaveUtfoertResultatResponseProjection.class));

        var input = requestCaptor.getValue().getInput();

        assertThat(input).containsOnlyKeys("id", "utfoertTidspunkt", "hardDelete", "nyLenke")
            .containsEntry("id", expectedId)
            .containsEntry("utfoertTidspunkt", expectedTidspunkt.format(DateTimeFormatter.ISO_DATE_TIME));
        assertThat(input.get("hardDelete")).isNull();
        assertThat(input.get("nyLenke")).isNull();
    }


    @Test
    void ferdigstill_sak() {
        var expectedId = "TestId";

        var requestCaptor = ArgumentCaptor.forClass(NyStatusSakMutationRequest.class);

        tjeneste.ferdigstillSak(expectedId, false);

        Mockito.verify(klient).oppdaterSakStatus(requestCaptor.capture(), any(NyStatusSakResultatResponseProjection.class));

        var request = requestCaptor.getValue();

        var input = request.getInput();

        assertThat(input).containsOnlyKeys("id", "overstyrStatustekstMed", "nyStatus", "idempotencyKey", "hardDelete", "tidspunkt", "nyLenkeTilSak")
            .containsEntry("id", expectedId)
            .containsEntry("nyStatus", SaksStatus.FERDIG)
            .containsEntry("overstyrStatustekstMed", "");

        assertThat(input.get("idempotencyKey")).isNull();
        assertThat(input.get("hardDelete")).isNull();
        assertThat(input.get("tidspunkt")).isNull();
        assertThat(input.get("nyLenkeTilSak")).isNull();
    }

    @Test
    void ferdigstill_arbeidsgiverinitiert_sak() {
        var expectedId = "TestId";

        var requestCaptor = ArgumentCaptor.forClass(NyStatusSakMutationRequest.class);

        tjeneste.ferdigstillSak(expectedId, true);

        Mockito.verify(klient).oppdaterSakStatus(requestCaptor.capture(), any(NyStatusSakResultatResponseProjection.class));

        var request = requestCaptor.getValue();

        var input = request.getInput();

        assertThat(input).containsOnlyKeys("id", "overstyrStatustekstMed", "nyStatus", "idempotencyKey", "hardDelete", "tidspunkt", "nyLenkeTilSak")
            .containsEntry("id", expectedId)
            .containsEntry("nyStatus", SaksStatus.FERDIG)
            .containsEntry("overstyrStatustekstMed", MinSideArbeidsgiverTjenesteImpl.SAK_STATUS_TEKST_ARBEIDSGIVERINITIERT);

        assertThat(input.get("idempotencyKey")).isNull();
        assertThat(input.get("hardDelete")).isNull();
        assertThat(input.get("tidspunkt")).isNull();
        assertThat(input.get("nyLenkeTilSak")).isNull();
    }

    @Test
    void oppdater_Tillegsinformasjon() {
        var expectedId = "TestId";
        var expectedTilleggsinformasjon = "Saksbehandler har gått videre uten din inntektsmelding";

        var requestCaptor = ArgumentCaptor.forClass(TilleggsinformasjonSakMutationRequest.class);

        tjeneste.oppdaterSakTilleggsinformasjon(expectedId, expectedTilleggsinformasjon);

        Mockito.verify(klient).oppdaterSakTilleggsinformasjon(requestCaptor.capture(), any(TilleggsinformasjonSakResultatResponseProjection.class));

        var request = requestCaptor.getValue();

        var input = request.getInput();

        assertThat(input).containsOnlyKeys("id", "idempotencyKey", "tilleggsinformasjon")
            .containsEntry("id", expectedId)
            .containsEntry("tilleggsinformasjon", expectedTilleggsinformasjon)
            .containsEntry("idempotencyKey", null);
    }
}
