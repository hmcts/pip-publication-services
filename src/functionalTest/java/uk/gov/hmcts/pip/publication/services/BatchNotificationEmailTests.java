package uk.gov.hmcts.pip.publication.services;

import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.pip.publication.services.utils.EmailNotificationClient;
import uk.gov.hmcts.pip.publication.services.utils.FunctionalTestBase;
import uk.gov.hmcts.pip.publication.services.utils.OAuthClient;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.NotificationList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.pip.publication.services.utils.EmailNotificationClient.NOTIFICATION_TYPE;

@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class, EmailNotificationClient.class})
class BatchNotificationEmailTests extends FunctionalTestBase {
    private static final String NOTIFY_URL = "/notify";
    private static final String LOCATION_SUBSCRIPTION_DELETION_EMAIL_URL = NOTIFY_URL + "/location-subscription-delete";

    private static final String TEST_EMAIL1 = "test_user1@justice.gov.uk";
    private static final String TEST_EMAIL2 = "test_user2@justice.gov.uk";
    private static final String TEST_LOCATION_NAME = "Test location name";

    @Autowired
    private EmailNotificationClient notificationClient;

    @Test
    void shouldSendLocationDeletionEmail() throws NotificationClientException {
        LocationSubscriptionDeletion requestBody = new LocationSubscriptionDeletion(
            TEST_LOCATION_NAME, List.of(TEST_EMAIL1, TEST_EMAIL2)
        );

        final Response response = doPostRequest(LOCATION_SUBSCRIPTION_DELETION_EMAIL_URL,
                                                Map.of(AUTHORIZATION, bearerToken),
                                                requestBody);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        String referenceId = response.getBody().asString();
        assertThat(referenceId)
            .isNotEmpty();

        Awaitility.with()
            .pollInterval(1, TimeUnit.SECONDS)
            .await()
            .until(() -> {
                NotificationList notificationList = notificationClient.getNotifications(
                    null, NOTIFICATION_TYPE, referenceId, null
                );
                return notificationList != null
                    && notificationList.getNotifications().size() == 2;
            });

        NotificationList notificationList = notificationClient.getNotifications(
            null, NOTIFICATION_TYPE, referenceId, null
        );
        Notification firstNotification = notificationList.getNotifications().get(0);
        Notification secondNotification = notificationList.getNotifications().get(0);

        assertThat(firstNotification.getEmailAddress().get())
            .as("Email address does not match")
            .isIn(TEST_EMAIL1, TEST_EMAIL2);

        assertThat(secondNotification.getEmailAddress().get())
            .as("Email address does not match")
            .isIn(TEST_EMAIL1, TEST_EMAIL2);

        assertThat(firstNotification.getSubject())
            .as("Email subject does not match")
            .hasValue(String.format("Subscription for %s has been deleted", TEST_LOCATION_NAME));

        assertThat(firstNotification.getBody())
            .as("Email body does not match")
            .contains(String.format("As part of routine maintenance of the court and tribunal hearings service "
                                        + "we have had to delete %s from our service, in doing so we have also had "
                                        + "to delete your subscriptions for this location.", TEST_LOCATION_NAME));
    }
}
