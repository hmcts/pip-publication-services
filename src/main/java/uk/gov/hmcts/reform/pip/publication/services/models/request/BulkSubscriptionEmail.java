package uk.gov.hmcts.reform.pip.publication.services.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import uk.gov.hmcts.reform.pip.publication.services.validation.SubscriptionsConstraint;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
