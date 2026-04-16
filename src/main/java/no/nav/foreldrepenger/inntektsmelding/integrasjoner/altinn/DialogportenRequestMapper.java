package no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.kvittering.PdfDokumentRest;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

public class DialogportenRequestMapper {
    private static final String ALTINN_RESSURS_PREFIX = "urn:altinn:resource:";

    private DialogportenRequestMapper(){
        //statisk klasse
    }

    public static DialogportenRequest opprettDialogRequest(Arbeidsgiver arbeidsgiver,
                                                           UUID forespørselUuid,
                                                           String sakstittel,
                                                           LocalDate førsteUttaksdato,
                                                           Ytelsetype ytelsetype,
                                                           String inntektsmeldingSkjemaLenke,
                                                           String inntektsmeldingApiLenke,
                                                           String forespørselApiLenke,
                                                           String dokumentasjonsLenke) {
        var party = String.format("urn:altinn:organization:identifier-no:%s", arbeidsgiver.orgnr());
        var altinnressursFP = ALTINN_RESSURS_PREFIX + AltinnRessurser.ALTINN_TRE_INNTEKTSMELDING_RESSURS;

        //Oppretter dialog
        var summaryDialog = String.format("Nav trenger inntektsmelding for å behandle søknad om %s med startdato %s.",
            ytelsetype.name().toLowerCase(),
            førsteUttaksdato);
        var contentDialog = new DialogportenRequest.Content(lagContentValue(sakstittel), lagContentValue(summaryDialog), null);

        //Oppretter transmission
        var contentTransmission = new DialogportenRequest.Content(lagContentValue("Send inn inntektsmelding"), null, null);
        var guiUrl = new DialogportenRequest.Url(inntektsmeldingSkjemaLenke + "/" + forespørselUuid.toString(), DialogportenRequest.NB,
            DialogportenRequest.AttachmentUrlConsumerType.Gui);
        var apiUrl = new DialogportenRequest.Url(inntektsmeldingApiLenke,
            DialogportenRequest.TEXT_PLAIN,
            DialogportenRequest.AttachmentUrlConsumerType.Api);
        var forespørselApiUrl = new DialogportenRequest.Url(forespørselApiLenke + "/" + forespørselUuid,
            DialogportenRequest.TEXT_PLAIN,
            DialogportenRequest.AttachmentUrlConsumerType.Api);
        var attachementTransmission = new DialogportenRequest.Attachment(
            List.of(new DialogportenRequest.ContentValueItem("Innsending av inntektsmelding på min side - arbeidsgiver hos Nav",
                DialogportenRequest.NB)),
            List.of(guiUrl, apiUrl, forespørselApiUrl));
        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Request,
            DialogportenRequest.ExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender("ServiceOwner", null),
            contentTransmission,
            List.of(attachementTransmission));

        //oppretter api action
        var apiAction = new DialogportenRequest.ApiAction(String.format("Innsending av inntektsmelding for %s med startdato %s",
            ytelsetype.name().toLowerCase(),
            førsteUttaksdato.format(DateTimeFormatter.ofPattern("dd.MM.yy"))),
            List.of(new DialogportenRequest.Endpoint(inntektsmeldingApiLenke, DialogportenRequest.HttpMethod.POST, dokumentasjonsLenke)),
            DialogportenRequest.ACTION_WRITE);

        return new DialogportenRequest(altinnressursFP,
            party,
            forespørselUuid.toString(),
            DialogportenRequest.DialogStatus.RequiresAttention,
            contentDialog,
            List.of(transmission),
            List.of(apiAction));
    }

    public static List<DialogportenPatchRequest> opprettFerdigstillPatchRequest(String sakstittel,
                                                                                Arbeidsgiver arbeidsgiver,
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

        var patchTransmission = inntektsmeldingMottattTransmission(arbeidsgiver, inntektsmeldingUuid, årsak, inntektsmeldingSkjemaLenke, true);

        return List.of(patchStatus, patchContent, patchTransmission);
    }

    public static List<DialogportenPatchRequest> opprettInnsendtInntektsmeldingPatchRequest(Arbeidsgiver arbeidsgiver,
                                                                                            Optional<UUID> inntektsmeldingUuid,
                                                                                            String inntektsmeldingSkjemaLenke) {
        var patchTransmission = inntektsmeldingMottattTransmission(arbeidsgiver,
            inntektsmeldingUuid,
            LukkeÅrsak.ORDINÆR_INNSENDING,
            inntektsmeldingSkjemaLenke,
            false);

        return List.of(patchTransmission);
    }

    private static DialogportenPatchRequest inntektsmeldingMottattTransmission(Arbeidsgiver arbeidsgiver,
                                                                               Optional<UUID> inntektsmeldingUuid,
                                                                               LukkeÅrsak årsak,
                                                                               String inntektsmeldingSkjemaLenke,
                                                                               boolean førsteInnsending) {
        //Ny transmission som sier at inntektsmelding er mottatt, og med en lenke til kvittering. Ekstern innsending har ingen kvittering.
        var mottattTekst = førsteInnsending ? "Inntektsmelding er mottatt" : "Oppdatert inntektsmelding er mottatt";
        var contentTransmission = årsak == LukkeÅrsak.EKSTERN_INNSENDING
                                  ? lagContentValue("Utført i Altinn eller i bedriftens lønns- og personalsystem. Ingen kvittering")
                                  : lagContentValue(mottattTekst);

        var transmissionContent = new DialogportenRequest.Content(contentTransmission, null, null);

        //attachement med kvittering
        var apiActions = inntektsmeldingUuid.map(imUuid -> {
            var innsendingTekst = førsteInnsending ? "Innsendt inntektsmelding" : "Oppdatert inntektsmelding";
            var contentAttachement = List.of(new DialogportenRequest.ContentValueItem(innsendingTekst, DialogportenRequest.NB));
            String url = new StringBuilder(inntektsmeldingSkjemaLenke)
                .append("/server/api")
                .append(PdfDokumentRest.INNTEKTSMELDING_FULL_PATH)
                .append("/")
                .append(imUuid).toString();
            var urlApi = new DialogportenRequest.Url(url, DialogportenRequest.TEXT_PLAIN, DialogportenRequest.AttachmentUrlConsumerType.Api);
            var urlGui = new DialogportenRequest.Url(url, DialogportenRequest.TEXT_PLAIN, DialogportenRequest.AttachmentUrlConsumerType.Gui);
            var attachment = new DialogportenRequest.Attachment(contentAttachement, List.of(urlApi, urlGui));
            return List.of(attachment);
        }).orElse(List.of());
        var actorId = String.format("urn:altinn:organization:identifier-no:%s", arbeidsgiver.orgnr());

        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Acceptance,
            DialogportenRequest.ExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender("PartyRepresentative", actorId),
            transmissionContent,
            apiActions);

        //patch
        return new DialogportenPatchRequest(DialogportenPatchRequest.OP_ADD,
            DialogportenPatchRequest.PATH_TRANSMISSIONS,
            List.of(transmission));
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
