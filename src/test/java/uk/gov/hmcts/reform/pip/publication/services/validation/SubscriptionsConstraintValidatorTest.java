package uk.gov.hmcts.reform.pip.publication.services.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SubscriptionsConstraintValidatorTest {

    SubscriptionsConstraintValidator subscriptionsConstraintValidator = new SubscriptionsConstraintValidator();

    @Test
    void testValid() {
        Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of("1234"));
        assertTrue(subscriptionsConstraintValidator.isValid(subscriptions, null), "Marked as invalid when valid");
    }

    @Test
    void testInvalid() {
        Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of());
        assertFalse(subscriptionsConstraintValidator.isValid(subscriptions, null), "Marked as valid when invalid");
    }

}
