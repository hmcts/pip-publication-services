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
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.NotificationList;

import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.pip.publication.services.utils.EmailNotificationClient.NOTIFICATION_TYPE;

@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class, EmailNotificationClient.class})
class NotificationEmailTests extends FunctionalTestBase {
    private static final String NOTIFY_URL = "/notify";
    private static final String MEDIA_WELCOME_EMAIL_URL = NOTIFY_URL + "/welcome-email";

    private static final String TEST_EMAIL = "test_user@justice.gov.uk";
    private static final String TEST_FULL_NAME = "test user";

    @Autowired
    private EmailNotificationClient notificationClient;

    @Test
    void shouldSendMediaWelcomeEmailToNewUser() throws NotificationClientException {
        WelcomeEmail requestBody = new WelcomeEmail(TEST_EMAIL, false, TEST_FULL_NAME);

        final Response response = doPostRequest(MEDIA_WELCOME_EMAIL_URL,
                                                Map.of(AUTHORIZATION, bearerToken),
                                                requestBody);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        String referenceId = response.getBody().asString();
        assertThat(referenceId)
            .isNotEmpty();

        Awaitility.with()
            .pollInterval(1, SECONDS)
            .await()
            .atMost(50, SECONDS)
            .until(() -> {
                NotificationList notificationList = notificationClient.getNotifications(
                    null, NOTIFICATION_TYPE, referenceId, null
                );
                return notificationList != null
                    && notificationList.getNotifications().size() == 1;
            });

        NotificationList notificationList = notificationClient.getNotifications(
            null, NOTIFICATION_TYPE, referenceId, null
        );
        Notification notification = notificationList.getNotifications().get(0);

        assertThat(notification.getEmailAddress())
            .as("Email address does not match")
            .hasValue(TEST_EMAIL);

        assertThat(notification.getSubject())
            .as("Email subject does not match")
            .hasValue("Court and tribunal hearings account – request approved");

        assertThat(notification.getBody())
            .as("Email body does not match")
            .contains("Click on the link to confirm your email address and finish creating your account");
    }
}
