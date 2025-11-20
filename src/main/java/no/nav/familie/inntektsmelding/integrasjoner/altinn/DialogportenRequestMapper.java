package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.konfig.Environment;

public class DialogportenRequestMapper {
    private static final String ALTINN_RESSURS_PREFIX = "urn:altinn:resource:";

    private DialogportenRequestMapper(){
        //statisk klasse
    }

    public static DialogportenRequest opprettDialogRequest(OrganisasjonsnummerDto organisasjonsnummer,
                                                           UUID forespørselUuid,
                                                           String sakstittel,
                                                           LocalDate førsteUttaksdato,
                                                           Ytelsetype ytelsetype,
                                                           String inntektsmeldingSkjemaLenke,
                                                           PersonIdent fødselsnummer) {
        var party = String.format("urn:altinn:organization:identifier-no:%s", organisasjonsnummer.orgnr());
        var foreldrepengerRessurs = Environment.current().getProperty("altinn.tre.inntektsmelding.ressurs");
        var altinnressursFP = ALTINN_RESSURS_PREFIX + foreldrepengerRessurs;

        //Oppretter dialog
        var summaryDialog = String.format("Nav trenger inntektsmelding for å behandle søknad om %s med startdato %s.",
            ytelsetype.name().toLowerCase(),
            førsteUttaksdato);
        var contentDialog = new DialogportenRequest.Content(lagContentValue(sakstittel), lagContentValue(summaryDialog), null);

        //Oppretter transmission
        var contentTransmission = new DialogportenRequest.Content(lagContentValue("Send inn inntektsmelding"), null, null);
        var attachementTransmission = new DialogportenRequest.Attachment(
            List.of(new DialogportenRequest.ContentValueItem("Innsending av inntektsmelding på min side - arbeidsgiver hos Nav",
                DialogportenRequest.NB)),
            List.of(new DialogportenRequest.Url(inntektsmeldingSkjemaLenke + "/" + forespørselUuid.toString(), DialogportenRequest.NB,
                DialogportenRequest.AttachmentUrlConsumerType.Gui)));
        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Request,
            DialogportenRequest.ExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender("ServiceOwner", null),
            contentTransmission,
            List.of(attachementTransmission));

        //oppretter api action
        var apiAction = new DialogportenRequest.ApiAction(String.format("Innsending av inntektsmelding for %s med startdato %s",
            ytelsetype.name().toLowerCase(),
            førsteUttaksdato.format(DateTimeFormatter.ofPattern("dd.MM.yy"))),
            List.of(new DialogportenRequest.Endpoint(inntektsmeldingSkjemaLenke + "/" + forespørselUuid, DialogportenRequest.HttpMethod.GET, null)),
            DialogportenRequest.ACTION_READ);

        return new DialogportenRequest(altinnressursFP,
            party,
            fødselsnummer.getIdent(),
            DialogportenRequest.DialogStatus.RequiresAttention,
            contentDialog,
            List.of(transmission),
            List.of(apiAction));
    }

    public static List<DialogportenPatchRequest> opprettFerdigstillPatchRequest(String sakstittel,
                                                                                OrganisasjonsnummerDto organisasjonsnummer,
                                                                                Ytelsetype ytelsetype,
                                                                                LocalDate førsteUttaksdato,
                                                                                Optional<UUID> inntektsmeldingUuid,
                                                                                LukkeÅrsak årsak,
                                                                                String inntektsmeldingSkjemaLenke) {
        //oppdatere status på meldingen til fullført
        var patchStatus = new DialogportenPatchRequest(DialogportenPatchRequest.OP_REPLACE,
            DialogportenPatchRequest.PATH_STATUS,
            DialogportenRequest.DialogStatus.Completed);

        //oppdatere innholdet i dialogen
        var summaryDialog = String.format("Nav har mottatt inntektsmelding for søknad om %s med startdato %s",
            ytelsetype.name().toLowerCase(),
            førsteUttaksdato.format(
                DateTimeFormatter.ofPattern("dd.MM.yy")));
        var contentRequest = new DialogportenRequest.Content(lagContentValue(sakstittel), lagContentValue(summaryDialog), null);
        var patchContent = new DialogportenPatchRequest(DialogportenPatchRequest.OP_REPLACE,
            DialogportenPatchRequest.PATH_CONTENT,
            contentRequest);

        //Ny transmission som sier at inntektsmelding er mottatt, og med en lenke til kvittering. Ekstern innsending har ingen kvittering.
        var contentTransmission = årsak == LukkeÅrsak.EKSTERN_INNSENDING
                                  ? lagContentValue("Utført i Altinn eller i bedriftens lønns- og personalsystem. Ingen kvittering")
                                  : lagContentValue("Inntektsmelding er mottatt ");

        var transmissionContent = new DialogportenRequest.Content(contentTransmission, null, null);

        //attachement med kvittering
        var apiActions = inntektsmeldingUuid.map(imUuid -> {
            var contentAttachement = List.of(new DialogportenRequest.ContentValueItem("Kvittering for inntektsmelding", DialogportenRequest.NB));
            var url = inntektsmeldingSkjemaLenke + "/server/api/ekstern/kvittering/inntektsmelding/" + imUuid;
            var urlApi = new DialogportenRequest.Url(url, DialogportenRequest.TEXT_PLAIN, DialogportenRequest.AttachmentUrlConsumerType.Api);
            var urlGui = new DialogportenRequest.Url(url, DialogportenRequest.TEXT_PLAIN, DialogportenRequest.AttachmentUrlConsumerType.Gui);
            var attachment = new DialogportenRequest.Attachment(contentAttachement, List.of(urlApi, urlGui));
            return List.of(attachment);
        }).orElse(List.of());
        var actorId = String.format("urn:altinn:organization:identifier-no:%s", organisasjonsnummer.orgnr());

        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Acceptance,
            DialogportenRequest.ExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender("PartyRepresentative", actorId),
            transmissionContent,
            apiActions);

        //patch
        var patchTransmission = new DialogportenPatchRequest(DialogportenPatchRequest.OP_ADD,
            DialogportenPatchRequest.PATH_TRANSMISSIONS,
            List.of(transmission));

        return List.of(patchStatus, patchContent, patchTransmission);
    }

    public static List<DialogportenPatchRequest> opprettUtgåttPatchRequest(String sakstittel) {
        //oppdatere status på dialogen til not applicable
        var patchStatus = new DialogportenPatchRequest(DialogportenPatchRequest.OP_REPLACE,
            DialogportenPatchRequest.PATH_STATUS,
            DialogportenRequest.DialogStatus.NotApplicable);

        //legger til extended status utgått fordi det ikke finnes en tilsvarende på dialogStatus
        //denne kan leses maskinelt av mottaker
        var patchExtendedStatus = new DialogportenPatchRequest(DialogportenPatchRequest.OP_REPLACE,
            DialogportenPatchRequest.PATH_EXTENDED_STATUS,
            DialogportenRequest.ExtendedDialogStatus.Utgått);

        //oppdatere innholdet i dialogen
        var contentRequest = new DialogportenRequest.Content(lagContentValue(sakstittel),
            lagContentValue("Nav trenger ikke lenger denne inntektsmeldingen"),
            lagContentValue("Utgått"));
        var patchContent = new DialogportenPatchRequest(DialogportenPatchRequest.OP_REPLACE,
            DialogportenPatchRequest.PATH_CONTENT,
            contentRequest);

        //Ny transmission som sier at inntektsmelding ikke lenger er påkrevd
        var transmissionContent = new DialogportenRequest.Content(lagContentValue("Inntektsmeldingen er ikke lenger påkrevd"), null, null);
        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Correction,
            DialogportenRequest.ExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender("ServiceOwner", null),
            transmissionContent,
            List.of());
        var patchTransmission = new DialogportenPatchRequest(DialogportenPatchRequest.OP_ADD,
            DialogportenPatchRequest.PATH_TRANSMISSIONS,
            List.of(transmission));

        return List.of(patchStatus, patchExtendedStatus, patchContent, patchTransmission);
    }

    private static DialogportenRequest.ContentValue lagContentValue(String verdi) {
        return new DialogportenRequest.ContentValue(List.of(new DialogportenRequest.ContentValueItem(verdi, DialogportenRequest.NB)),
            DialogportenRequest.TEXT_PLAIN);
    }
}
