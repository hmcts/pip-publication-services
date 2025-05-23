package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.LocationSubscriptionDeletionEmailData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.DELETE_LOCATION_SUBSCRIPTION;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class LocationSubscriptionDeletionEmailGeneratorTest {
    private static final String EMAIL1 = "test1@testing.com";
    private static final String EMAIL2 = "test2@testing.com";
    private static final String EMAIL3 = "test3@testing.com";
    private static final String LOCATION_NAME = "Location name";
    private static final String REFERENCE_ID = UUID.randomUUID().toString();
    private static final String LOCATION_NAME_PERSONALISATION = "location-name";

    private static final String RESULTS_MESSAGE = "Returned result size does not match";
    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String REFERENCE_ID_MESSAGE = "Reference ID does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    @Mock
    private PersonalisationLinks personalisationLinks;

    @InjectMocks
    private LocationSubscriptionDeletionEmailGenerator emailGenerator;

    @Test
    void testBuildLocationSubscriptionDeletionEmail() {
        LocationSubscriptionDeletionEmailData emailData = new LocationSubscriptionDeletionEmailData(
            List.of(EMAIL1, EMAIL2, EMAIL3), LOCATION_NAME, REFERENCE_ID
        );

        List<EmailToSend> results = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(results)
            .as(RESULTS_MESSAGE)
            .hasSize(3);

        softly.assertThat(results.get(0).getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL1);

        softly.assertThat(results.get(1).getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL2);

        softly.assertThat(results.get(2).getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL3);

        softly.assertThat(results.get(0).getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(DELETE_LOCATION_SUBSCRIPTION.getTemplate());

        softly.assertThat(results.get(0).getReferenceId())
            .as(REFERENCE_ID_MESSAGE)
            .isEqualTo(REFERENCE_ID);

        Map<String, Object> personalisation = results.get(0).getPersonalisation();

        softly.assertThat(personalisation.get(LOCATION_NAME_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(LOCATION_NAME);

        softly.assertAll();
    }
}
