package uk.gov.hmcts.reform.pip.publication.services.models.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Bulk subscriptions class which contains the artefactID and a list of subscription emails.
 */
@Data
public class BulkSubscriptionEmail {

    @NotNull
    UUID artefactId;

    @NotEmpty
    List<SubscriptionEmail> subscriptionEmails;
}
