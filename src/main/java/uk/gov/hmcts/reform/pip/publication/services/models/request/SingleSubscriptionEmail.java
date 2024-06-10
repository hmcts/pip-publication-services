package uk.gov.hmcts.reform.pip.publication.services.models.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * This class is used for the deprecated endpoint when sending a single subscription email.
 * This will be removed once migrated over to using the new endpoint.
 */
@Deprecated
@Getter
@Setter
public class SingleSubscriptionEmail extends SubscriptionEmail {

    @NotNull
    UUID artefactId;
}
