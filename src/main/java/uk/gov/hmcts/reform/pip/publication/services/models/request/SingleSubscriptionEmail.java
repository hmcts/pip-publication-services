package uk.gov.hmcts.reform.pip.publication.services.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.validation.SubscriptionsConstraint;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
@Getter
@Setter
public class SingleSubscriptionEmail extends SubscriptionEmail {

    @NotNull
    UUID artefactId;
}
