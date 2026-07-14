package uk.gov.hmcts.reform.pip.publication.services.models.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Bulk subscriptions class which contains the artefactID and a list of subscription emails.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkSubscriptionEmail {

    @NotNull
    private UUID artefactId;

    @NotEmpty
    private List<SubscriptionEmail> subscriptionEmails;
}
