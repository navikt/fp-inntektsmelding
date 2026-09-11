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
    private static final String SERVICE_OWNER = "ServiceOwner";
    private static final FlerspråkligTekst PURRING_TITTEL = new FlerspråkligTekst(
        "Vi har ennå ikke mottatt inntektsmelding",
        "Vi har enno ikkje motteke inntektsmelding",
        "We have not yet received the income statement");
    private static final FlerspråkligTekst SEND_INN_INNTEKTSMELDING = new FlerspråkligTekst(
        "Send inn inntektsmelding",
        "Send inn inntektsmelding",
        "Submit income statement");
    private static final FlerspråkligTekst INNSENDING_PA_MIN_SIDE = new FlerspråkligTekst(
        "Innsending av inntektsmelding på min side - arbeidsgiver hos Nav",
        "Innsending av inntektsmelding på Min side - arbeidsgivar hos Nav",
        "Submission of the income statement on Min side – arbeidsgiver (Nav's employer portal)");

    private DialogportenRequestMapper(){
        //statisk klasse
    }

    public static DialogportenRequest opprettDialogRequest(Arbeidsgiver arbeidsgiver,
                                                           UUID forespørselUuid,
                                                           FlerspråkligTekst sakstittel,
                                                           LocalDate førsteUttaksdato,
                                                           Ytelsetype ytelsetype,
                                                           String inntektsmeldingSkjemaLenke,
                                                           String inntektsmeldingApiLenke,
                                                           String forespørselApiLenke,
                                                           String dokumentasjonsLenke) {
        var party = String.format("urn:altinn:organization:identifier-no:%s", arbeidsgiver.orgnr());
        var altinnressursFP = ALTINN_RESSURS_PREFIX + AltinnRessurser.ALTINN_TRE_INNTEKTSMELDING_RESSURS;
        var ytelsesnavn = mapYtelsestypeNavn(ytelsetype);

        //Oppretter dialog
        var summaryDialog = new FlerspråkligTekst(
            "Nav trenger inntektsmelding for å behandle søknad om %s med startdato %s.".formatted(ytelsesnavn.nb(), formaterDato(førsteUttaksdato)),
            "Nav treng inntektsmelding for å behandle søknaden om %s med startdato %s.".formatted(ytelsesnavn.nn(), formaterDato(førsteUttaksdato)),
            "Nav needs an income statement to process the application for %s with a start date of %s.".formatted(ytelsesnavn.en(), formaterDato(førsteUttaksdato)));
        var contentDialog = new DialogportenRequest.Content(lagContentValue(sakstittel), lagContentValue(summaryDialog), null);

        //Oppretter transmission
        var contentTransmission = new DialogportenRequest.Content(lagContentValue(SEND_INN_INNTEKTSMELDING), null, null);
        var guiUrl = new DialogportenRequest.Url(inntektsmeldingSkjemaLenke + "/" + forespørselUuid.toString(), DialogportenRequest.TEXT_PLAIN,
            DialogportenRequest.AttachmentUrlConsumerType.Gui);
        var forespørselApiUrl = new DialogportenRequest.Url(forespørselApiLenke + "/" + forespørselUuid,
            DialogportenRequest.TEXT_PLAIN,
            DialogportenRequest.AttachmentUrlConsumerType.Api);
        var attachementTransmission = new DialogportenRequest.Attachment(
            lagContentValueItems(INNSENDING_PA_MIN_SIDE),
            List.of(guiUrl, forespørselApiUrl));
        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Request,
            DialogportenRequest.TransmissionExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender(SERVICE_OWNER, null),
            contentTransmission,
            List.of(attachementTransmission));

        //oppretter api action (kun ett navn tillatt, bruker bokmål siden dette ikke er et rent visningsfelt for sluttbruker)
        var apiActionNavn = "Innsending av inntektsmelding for %s med startdato %s".formatted(ytelsesnavn.nb(), formaterDato(førsteUttaksdato));
        var apiAction = new DialogportenRequest.ApiAction(apiActionNavn,
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

    public static List<DialogportenPatchRequest>  opprettFerdigstillPatchRequest(FlerspråkligTekst sakstittel,
                                                                                 Arbeidsgiver arbeidsgiver,
                                                                                 Ytelsetype ytelsetype,
                                                                                 LocalDate førsteUttaksdato,
                                                                                 Optional<UUID> inntektsmeldingUuid,
                                                                                 LukkeÅrsak årsak,
                                                                                 String inntektsmeldingSkjemaLenke,
                                                                                 String hentInntektsmeldingApiLenke) {
        //oppdatere status på meldingen til fullført
        var patchStatus = new DialogportenPatchRequest(DialogportenPatchRequest.OP_REPLACE,
            DialogportenPatchRequest.PATH_STATUS,
            DialogportenRequest.DialogStatus.Completed);

        //oppdatere innholdet i dialogen
        var ytelsesnavn = mapYtelsestypeNavn(ytelsetype);
        var summaryDialog = new FlerspråkligTekst(
            "Nav har mottatt inntektsmelding for søknad om %s med startdato %s".formatted(ytelsesnavn.nb(), formaterDato(førsteUttaksdato)),
            "Nav har motteke inntektsmelding for søknaden om %s med startdato %s".formatted(ytelsesnavn.nn(), formaterDato(førsteUttaksdato)),
            "Nav has received the income statement for the application for %s with a start date of %s".formatted(ytelsesnavn.en(), formaterDato(førsteUttaksdato)));
        var contentRequest = new DialogportenRequest.Content(lagContentValue(sakstittel), lagContentValue(summaryDialog), null);
        var patchContent = new DialogportenPatchRequest(DialogportenPatchRequest.OP_REPLACE,
            DialogportenPatchRequest.PATH_CONTENT,
            contentRequest);

        var patchTransmission = inntektsmeldingMottattTransmission(arbeidsgiver, inntektsmeldingUuid, årsak, inntektsmeldingSkjemaLenke,
            hentInntektsmeldingApiLenke,
            true);

        return List.of(patchStatus, patchContent, patchTransmission);
    }

    public static List<DialogportenPatchRequest> opprettInnsendtInntektsmeldingPatchRequest(Arbeidsgiver arbeidsgiver,
                                                                                            Optional<UUID> inntektsmeldingUuid,
                                                                                            String inntektsmeldingSkjemaLenke,
                                                                                            String hentInntektsmeldingApiLenke) {
        var patchTransmission = inntektsmeldingMottattTransmission(arbeidsgiver,
            inntektsmeldingUuid,
            LukkeÅrsak.ORDINÆR_INNSENDING,
            inntektsmeldingSkjemaLenke,
            hentInntektsmeldingApiLenke,
            false);

        return List.of(patchTransmission);
    }

    private static DialogportenPatchRequest inntektsmeldingMottattTransmission(Arbeidsgiver arbeidsgiver,
                                                                               Optional<UUID> inntektsmeldingUuid,
                                                                               LukkeÅrsak årsak,
                                                                               String inntektsmeldingSkjemaLenke,
                                                                               String hentInntektsmeldingApiLenke,
                                                                               boolean førsteInnsending) {
        //Ny transmission som sier at inntektsmelding er mottatt, og med en lenke til kvittering. Ekstern innsending har ingen kvittering.
        var mottattTekst = førsteInnsending
                           ? new FlerspråkligTekst("Inntektsmelding er mottatt", "Inntektsmelding er motteke", "Income statement received")
                           : new FlerspråkligTekst("Oppdatert inntektsmelding er mottatt", "Oppdatert inntektsmelding er motteke", "Updated income statement received");
        var eksternInnsendingTekst = new FlerspråkligTekst(
            "Utført i Altinn eller i bedriftens lønns- og personalsystem. Ingen kvittering",
            "Utført i Altinn eller i verksemda sitt løns- og personalsystem. Ingen kvittering",
            "Submitted via Altinn or the employer's payroll/HR system. No receipt");
        var contentTransmission = årsak == LukkeÅrsak.EKSTERN_INNSENDING
                                  ? lagContentValue(eksternInnsendingTekst)
                                  : lagContentValue(mottattTekst);

        var transmissionContent = new DialogportenRequest.Content(contentTransmission, null, null);

        //attachement med kvittering
        var attachements = inntektsmeldingUuid.map(imUuid -> {
            var innsendingTekst = førsteInnsending
                                  ? new FlerspråkligTekst("Innsendt inntektsmelding", "Innsend inntektsmelding", "Submitted income statement")
                                  : new FlerspråkligTekst("Oppdatert inntektsmelding", "Oppdatert inntektsmelding", "Updated income statement");
            var contentAttachement = lagContentValueItems(innsendingTekst);
            String urlPdf = new StringBuilder(inntektsmeldingSkjemaLenke)
                .append("/server/api")
                .append(PdfDokumentRest.INNTEKTSMELDING_FULL_PATH)
                .append("/")
                .append(imUuid).toString();
            var urlJson = new StringBuilder(hentInntektsmeldingApiLenke)
                .append("/")
                .append(imUuid).toString();
            var urlApi = new DialogportenRequest.Url(urlJson, DialogportenRequest.APPLICATION_JSON, DialogportenRequest.AttachmentUrlConsumerType.Api);
            var urlGui = new DialogportenRequest.Url(urlPdf, DialogportenRequest.APPLICATION_PDF, DialogportenRequest.AttachmentUrlConsumerType.Gui);
            var attachment = new DialogportenRequest.Attachment(contentAttachement, List.of(urlApi, urlGui));
            return List.of(attachment);
        }).orElse(List.of());
        var actorId = String.format("urn:altinn:organization:identifier-no:%s", arbeidsgiver.orgnr());

        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Acceptance,
            DialogportenRequest.TransmissionExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender("PartyRepresentative", actorId),
            transmissionContent,
            attachements);

        //patch
        return new DialogportenPatchRequest(DialogportenPatchRequest.OP_ADD,
            DialogportenPatchRequest.PATH_TRANSMISSIONS,
            List.of(transmission));
    }

    public static DialogportenPatchRequest opprettEndretFørsteUttaksdatoPatchRequest(String beskjedTekst) {
        var transmissionContent = new DialogportenRequest.Content(lagContentValue(beskjedTekst), null, null);
        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Information,
            DialogportenRequest.TransmissionExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender(SERVICE_OWNER, null),
            transmissionContent,
            List.of());
        return new DialogportenPatchRequest(DialogportenPatchRequest.OP_ADD,
            DialogportenPatchRequest.PATH_TRANSMISSIONS,
            List.of(transmission));
    }

    public static DialogportenPatchRequest inntektsmeldingPurringMelding(FlerspråkligTekst purringTekst) {
        var transmissionContent = new DialogportenRequest.Content(lagContentValue(PURRING_TITTEL), lagContentValue(purringTekst), null);

        return new DialogportenPatchRequest(DialogportenPatchRequest.OP_ADD,
            DialogportenPatchRequest.PATH_TRANSMISSIONS,
            List.of(new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Request,
                DialogportenRequest.TransmissionExtendedType.INNTEKTSMELDING,
                new DialogportenRequest.Sender(SERVICE_OWNER, null),
                transmissionContent,
                List.of())));
    }

    public static DialogportenPatchRequest inntektsmeldingAvvistTransmission(Arbeidsgiver arbeidsgiver,
                                                                               FlerspråkligTekst avvistTekst) {
        var contentTransmission = lagContentValue(avvistTekst);

        var transmissionContent = new DialogportenRequest.Content(contentTransmission, null, null);

        var actorId = String.format("urn:altinn:organization:identifier-no:%s", arbeidsgiver.orgnr());

        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Rejection,
            DialogportenRequest.TransmissionExtendedType.INNTEKTSMELDING_AVVIST,
            new DialogportenRequest.Sender("PartyRepresentative", actorId),
            transmissionContent,
            List.of());

        // patch
        return new DialogportenPatchRequest(DialogportenPatchRequest.OP_ADD,
            DialogportenPatchRequest.PATH_TRANSMISSIONS,
            List.of(transmission));
    }

    public static List<DialogportenPatchRequest> opprettUtgåttPatchRequest(FlerspråkligTekst sakstittel) {
        //oppdatere status på dialogen til not applicable
        var patchStatus = new DialogportenPatchRequest(DialogportenPatchRequest.OP_REPLACE,
            DialogportenPatchRequest.PATH_STATUS,
            DialogportenRequest.DialogStatus.NotApplicable);

        //legger til extended status utgått fordi det ikke finnes en tilsvarende på dialogStatus
        //denne kan leses maskinelt av mottaker
        var patchExtendedStatus = new DialogportenPatchRequest(DialogportenPatchRequest.OP_REPLACE,
            DialogportenPatchRequest.PATH_EXTENDED_STATUS,
            DialogportenRequest.ExtendedDialogStatus.FORESPOERSEL_UTGAATT);

        //oppdatere innholdet i dialogen
        var utgåttSummary = new FlerspråkligTekst(
            "Nav trenger ikke lenger denne inntektsmeldingen",
            "Nav treng ikkje lenger denne inntektsmeldinga",
            "Nav no longer needs this income statement");
        var utgåttStatus = new FlerspråkligTekst("Utgått", "Utgått", "Expired");
        var contentRequest = new DialogportenRequest.Content(lagContentValue(sakstittel),
            lagContentValue(utgåttSummary),
            lagContentValue(utgåttStatus));
        var patchContent = new DialogportenPatchRequest(DialogportenPatchRequest.OP_REPLACE,
            DialogportenPatchRequest.PATH_CONTENT,
            contentRequest);

        //Ny transmission som sier at inntektsmelding ikke lenger er påkrevd
        var ikkePåkrevdTekst = new FlerspråkligTekst(
            "Inntektsmeldingen er ikke lenger påkrevd",
            "Inntektsmeldinga er ikkje lenger påkravd",
            "The income statement is no longer required");
        var transmissionContent = new DialogportenRequest.Content(lagContentValue(ikkePåkrevdTekst), null, null);
        var transmission = new DialogportenRequest.Transmission(DialogportenRequest.TransmissionType.Correction,
            DialogportenRequest.TransmissionExtendedType.INNTEKTSMELDING,
            new DialogportenRequest.Sender(SERVICE_OWNER, null),
            transmissionContent,
            List.of());
        var patchTransmission = new DialogportenPatchRequest(DialogportenPatchRequest.OP_ADD,
            DialogportenPatchRequest.PATH_TRANSMISSIONS,
            List.of(transmission));

        return List.of(patchStatus, patchExtendedStatus, patchContent, patchTransmission);
    }

    private static DialogportenRequest.ContentValue lagContentValue(FlerspråkligTekst tekst) {
        return new DialogportenRequest.ContentValue(lagContentValueItems(tekst), DialogportenRequest.TEXT_PLAIN);
    }

    private static List<DialogportenRequest.ContentValueItem> lagContentValueItems(FlerspråkligTekst tekst) {
        return List.of(new DialogportenRequest.ContentValueItem(tekst.nb(), DialogportenRequest.NB),
            new DialogportenRequest.ContentValueItem(tekst.nn(), DialogportenRequest.NN),
            new DialogportenRequest.ContentValueItem(tekst.en(), DialogportenRequest.EN));
    }

    private static FlerspråkligTekst mapYtelsestypeNavn(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> new FlerspråkligTekst("foreldrepenger", "foreldrepengar", "parental benefit");
            case SVANGERSKAPSPENGER -> new FlerspråkligTekst("svangerskapspenger", "svangerskapspengar", "pregnancy benefit");
        };
    }

    private static String formaterDato(LocalDate dato) {
        return dato.format(DateTimeFormatter.ofPattern("dd.MM.yy"));
    }
}
