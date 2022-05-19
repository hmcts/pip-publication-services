package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.Data;
import uk.gov.hmcts.reform.pip.publication.services.validation.SubscriptionsConstraint;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

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
