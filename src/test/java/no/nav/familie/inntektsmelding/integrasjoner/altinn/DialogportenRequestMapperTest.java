package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

class DialogportenRequestMapperTest {
    private static final String SERVICE_RESOURCE = "urn:altinn:resource:nav_foreldrepenger_inntektsmelding";
    private final OrganisasjonsnummerDto ORGANISASJONSNUMMER = new OrganisasjonsnummerDto("999999999");
    private final UUID FORESPØRSEL_UUID = UUID.randomUUID();
    private final String INNTEKTSMELDING_SKJEMA_LENKE = "https://arbeidsgiver.nav.no/fp-im-dialog";
    private final LocalDate FØRSTE_UTTAKSDATO = LocalDate.now().plusWeeks(4);

    @BeforeEach
    void setUp() {
    }

    @Test
    void opprettDialogRequest() {
        var party = "urn:altinn:organization:identifier-no:999999999";

        var opprettRequest = DialogportenRequestMapper.opprettDialogRequest(ORGANISASJONSNUMMER,
            FORESPØRSEL_UUID, "Sakstittel", FØRSTE_UTTAKSDATO, Ytelsetype.FORELDREPENGER, INNTEKTSMELDING_SKJEMA_LENKE);

        var transmissionContent = opprettRequest.transmissions().getFirst().content().title().value().getFirst().value();
        var attachment = opprettRequest.transmissions().getFirst().attachments().getFirst();
        var attachmentName = attachment.displayName().getFirst().value();
        var attachmentUrl = attachment.urls().getFirst().url();
        var apiActionEndpointUrl = opprettRequest.apiActions().getFirst().endpoints().getFirst().url();

        assertThat(opprettRequest.party()).isEqualTo(party);
        assertThat(opprettRequest.serviceResource()).isEqualTo(SERVICE_RESOURCE);
        assertThat(opprettRequest.status()).isEqualTo(DialogportenRequest.DialogStatus.RequiresAttention);
        assertThat(opprettRequest.transmissions()).hasSize(1);
        assertThat(opprettRequest.apiActions()).hasSize(1);
        assertThat(opprettRequest.content().title().value().getFirst().value()).isEqualTo("Sakstittel");
        assertThat(attachmentName).isEqualTo("Innsending av inntektsmelding på min side - arbeidsgiver hos Nav");
        assertThat(attachmentUrl).isEqualTo(INNTEKTSMELDING_SKJEMA_LENKE + "/" + FORESPØRSEL_UUID);
        assertThat(apiActionEndpointUrl).isEqualTo(INNTEKTSMELDING_SKJEMA_LENKE + "/" + FORESPØRSEL_UUID);
        assertThat(transmissionContent).isEqualTo("Send inn inntektsmelding");
    }

    @Test
    void opprettFerdigstillPatchRequest() {
        var ferdigstillPatchRequest = DialogportenRequestMapper.opprettFerdigstillPatchRequest("Sakstittel",
            Ytelsetype.FORELDREPENGER,
            FØRSTE_UTTAKSDATO,
            Optional.of(FORESPØRSEL_UUID),
            null,
            INNTEKTSMELDING_SKJEMA_LENKE);

        var ops = ferdigstillPatchRequest.stream().map(DialogportenPatchRequest::op).toList();
        var paths = ferdigstillPatchRequest.stream().map(DialogportenPatchRequest::path).toList();
        var patchValue = ferdigstillPatchRequest.stream().map(DialogportenPatchRequest::value).toList();

        assertThat(ferdigstillPatchRequest).hasSize(3);
        assertThat(ops).contains(DialogportenPatchRequest.OP_ADD, DialogportenPatchRequest.OP_REPLACE);
        assertThat(paths).contains(DialogportenPatchRequest.PATH_STATUS,
            DialogportenPatchRequest.PATH_CONTENT,
            DialogportenPatchRequest.PATH_TRANSMISSIONS);
        assertThat(ferdigstillPatchRequest.getFirst().op()).isEqualTo(DialogportenPatchRequest.OP_REPLACE);
        assertThat(ferdigstillPatchRequest.get(1).value().toString()).contains("Nav har mottatt inntektsmelding for søknad om foreldrepenger med startdato %s".formatted(
            FØRSTE_UTTAKSDATO.format(DateTimeFormatter.ofPattern("dd.MM.yy"))));
        assertThat(ferdigstillPatchRequest.get(2).op()).isEqualTo(DialogportenPatchRequest.OP_ADD);
        assertThat(ferdigstillPatchRequest.get(2).path()).isEqualTo(DialogportenPatchRequest.PATH_TRANSMISSIONS);
        assertThat(patchValue).hasSize(3);
        assertThat(patchValue.toString()).contains("Completed");
        assertThat(patchValue.toString()).contains("Inntektsmelding er mottatt");
        assertThat(patchValue.toString()).contains("Kvittering for inntektsmelding");
        assertThat(patchValue.toString()).contains("url=https://arbeidsgiver.nav.no/fp-im-dialog/server/api/ekstern/kvittering/inntektsmelding/");
    }

    @Test
    void opprettFerdigstillPatchRequestLukketEksternt() {
        var ferdigstillPatchRequest = DialogportenRequestMapper.opprettFerdigstillPatchRequest("Sakstittel",
            Ytelsetype.FORELDREPENGER,
            FØRSTE_UTTAKSDATO,
            Optional.of(FORESPØRSEL_UUID),
            LukkeÅrsak.EKSTERN_INNSENDING,
            INNTEKTSMELDING_SKJEMA_LENKE);

        var ops = ferdigstillPatchRequest.stream().map(DialogportenPatchRequest::op).toList();
        var paths = ferdigstillPatchRequest.stream().map(DialogportenPatchRequest::path).toList();
        var patchValue = ferdigstillPatchRequest.stream().map(DialogportenPatchRequest::value).toList();

        assertThat(ferdigstillPatchRequest).hasSize(3);
        assertThat(ops).contains(DialogportenPatchRequest.OP_ADD, DialogportenPatchRequest.OP_REPLACE);
        assertThat(paths).contains(DialogportenPatchRequest.PATH_STATUS,
            DialogportenPatchRequest.PATH_CONTENT,
            DialogportenPatchRequest.PATH_TRANSMISSIONS);
        assertThat(ferdigstillPatchRequest.getFirst().op()).isEqualTo(DialogportenPatchRequest.OP_REPLACE);
        assertThat(ferdigstillPatchRequest.get(1).value().toString()).contains("Nav har mottatt inntektsmelding for søknad om foreldrepenger med startdato %s".formatted(
            FØRSTE_UTTAKSDATO.format(DateTimeFormatter.ofPattern("dd.MM.yy"))));
        assertThat(ferdigstillPatchRequest.get(2).op()).isEqualTo(DialogportenPatchRequest.OP_ADD);
        assertThat(ferdigstillPatchRequest.get(2).path()).isEqualTo(DialogportenPatchRequest.PATH_TRANSMISSIONS);
        assertThat(patchValue).hasSize(3);
        assertThat(patchValue.toString()).contains("Completed");
        assertThat(patchValue.toString()).contains("Utført i Altinn eller i bedriftens lønns- og personalsystem. Ingen kvittering");
    }

    @Test
    void opprettUtgåttPatchRequest() {
        var utgåttRequest = DialogportenRequestMapper.opprettUtgåttPatchRequest("sakstittel");

        var ops = utgåttRequest.stream().map(DialogportenPatchRequest::op).toList();
        var paths = utgåttRequest.stream().map(DialogportenPatchRequest::path).toList();
        var patchValue = utgåttRequest.stream().map(DialogportenPatchRequest::value).toList();

        assertThat(utgåttRequest).hasSize(4);
        assertThat(ops).contains(DialogportenPatchRequest.OP_REPLACE, DialogportenPatchRequest.OP_ADD);
        assertThat(paths).contains(DialogportenPatchRequest.PATH_STATUS,
            DialogportenPatchRequest.PATH_CONTENT,
            DialogportenPatchRequest.PATH_TRANSMISSIONS,
            DialogportenPatchRequest.PATH_EXTENDED_STATUS);
        assertThat(patchValue).hasSize(4);
        assertThat(patchValue.toString()).contains("NotApplicable");
        assertThat(patchValue.toString()).contains("Utgått");
        assertThat(patchValue.toString()).contains("Nav trenger ikke lenger denne inntektsmeldingen");
    }
}
