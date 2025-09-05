package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record DialogportenRequest(@NotNull String serviceResource,
                                  @NotNull String party,
                                  String externalRefererence,
                                  DialogStatus status,
                                  Content content,
                                  List<Transmission> transmissions,
                                  List<ApiAction> apiActions) {

    public static final String TEXT_PLAIN = "text/plain";
    public static final String NB = "nb";
    public static final String ACTION_READ = "read";

    enum DialogStatus {
        InProgress,
        Draft,
        RequiresAttention,
        Completed,
        Awaiting,
        NotApplicable,
    }
    enum ExtendedDialogStatus {
        Expired,
    }

    protected record ApiAction(String name, List<Endpoint> endpoints, String action) {
    }

    protected record Endpoint(String url, HttpMethod httpMethod, String documentationUrl) {
    }

    enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE,
        PATCH,
    }

    protected record Transmission(@NotNull TransmissionType type, ExtendedType extendedType, Sender sender, Content content, List<Attachment> attachments) {
    }

    enum TransmissionType {
        // For general information, not related to any submissions
        Information,

        // Feedback/receipt accepting a previous submission
        Acceptance,

        // Feedback/error message rejecting a previous submission
        Rejection,

        // Question/request for more information
        Request,

        Alert,
        Decision,
        Submission,
        Correction
    }

    enum ExtendedType {
        SYKMELDING,
        SYKEPENGESOEKNAD,
        INNTEKTSMELDING,
    }

    protected record Sender(@NotNull String actorType) {
    }

    protected record Content(@NotNull @Valid ContentValue title, @Valid ContentValue summary, @Valid ExtendedStatus extendedStatus) {
    }

    protected record ContentValue(@NotNull @NotEmpty @Valid List<ContentValueItem> value, @NotNull String mediaType) {
    }

    protected record ContentValueItem(@NotNull String value, @NotNull String languageCode) {
    }

    protected record Attachment(@Valid List<ContentValueItem> displayName, @Valid List<Url> urls) {
    }

    protected record Url(String url, String mediaType, AttachmentUrlConsumerType consumerType) {
    }

    protected record ExtendedStatus(ContentValueItem value, String mediaType) {
    }
    protected enum AttachmentUrlConsumerType {
        Gui,
        Api,
    }
}
