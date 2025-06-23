package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import java.util.List;

public record DialogportenRequest (String serviceResource, String party, String externalRefererence, DialogStatus status, Content content, List<Transmission> transmissions){

    enum DialogStatus {
        New,
        InProgress,
        Draft,
        Sent,
        RequiresAttention,
        Completed,
    }

    protected record Transmission(TransmissionType type, ExtendedType extendedType, Sender sender, Content content){}

    enum TransmissionType {
        // For general information, not related to any submissions
        Information,

        // Feedback/receipt accepting a previous submission
        Acceptance,

        // Feedback/error message rejecting a previous submission
        Rejection,

        // Question/request for more information
        Request,
    }

    enum ExtendedType {
        SYKMELDING,
        SYKEPENGESOEKNAD,
        INNTEKTSMELDING,
    }

    protected record Sender(String actorType){}

    protected record Content(ContentValue title, ContentValue summary){}

    protected record ContentValue(List<ContentValueItem> value) {
        public static final String mediaType = "text/plain";
    }

    protected record ContentValueItem(String value) {
        public static final String languageCode = "nb";
    }
}
