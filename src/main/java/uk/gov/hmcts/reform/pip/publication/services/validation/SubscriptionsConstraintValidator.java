package uk.gov.hmcts.reform.pip.publication.services.validation;

import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;

import java.util.List;
import java.util.Map;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SubscriptionsConstraintValidator implements ConstraintValidator<SubscriptionsConstraint,
    Map<SubscriptionTypes, List<String>>> {

    /**
     * Validates that the map has at least one element that has a subscription value.
     */
    @Override
    public boolean isValid(Map<SubscriptionTypes, List<String>> subscriptions, ConstraintValidatorContext cxt) {
        return subscriptions.values().stream().anyMatch(value -> !value.isEmpty());
    }

}
