package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import java.util.List;

public record DialogportenRequest (String serviceResource, String party, String externalRefererence, DialogStatus status, Content content, List<Transmission> transmissions, List<ApiAction> apiActions) {

    enum DialogStatus {
        New,
        InProgress,
        Draft,
        Sent,
        RequiresAttention,
        Completed,
    }

    protected record ApiAction(String name, List<Endpoint> endpoints) {
        public static final String action = "read";
    }

    protected record Endpoint(String url, HttpMethod httpMethod, String documentationUrl) {}

    enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE,
        PATCH,
    }

    protected record Transmission(TransmissionType type, ExtendedType extendedType, Sender sender, Content content, List<Attachment> attachments) {}

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

    protected record Attachment(List<ContentValueItem> displayName, List<Url> urls) {}
    protected record Url(String url, String mediaType, AttachmentUrlConsumerType consumerType) {}

    protected enum AttachmentUrlConsumerType {
        Gui,
        Api,
    }
}
