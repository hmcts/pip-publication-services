package uk.gov.hmcts.reform.pip.publication.services.models.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;

import java.util.List;

/**
 * Bulk subscriptions class which contains the artefact and a list of subscription emails.
 */
@Data
public class BulkSubscriptionEmailV2 {
    @NotNull
    Artefact artefact;

    @NotEmpty
    List<SubscriptionEmail> subscriptionEmails;
}
