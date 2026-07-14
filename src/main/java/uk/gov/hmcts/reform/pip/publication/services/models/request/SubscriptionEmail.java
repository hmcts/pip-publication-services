package uk.gov.hmcts.reform.pip.publication.services.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pip.publication.services.validation.SubscriptionsConstraint;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEmail {

    @Email
    @NotNull
    private String email;

    @SubscriptionsConstraint
    private Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();
}
