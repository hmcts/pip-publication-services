package uk.gov.hmcts.reform.pip.publication.services.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uk.gov.hmcts.reform.pip.publication.services.validation.SubscriptionsConstraint;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class SubscriptionEmail {

    @Email
    @NotNull
    String email;

    @SubscriptionsConstraint
    Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();

    @NotNull
    UUID artefactId;

}
