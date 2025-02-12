package uk.gov.hmcts.reform.pip.publication.services;

import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.request.OtpEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.utils.EmailNotificationClient;
import uk.gov.hmcts.reform.pip.publication.services.utils.OAuthClient;
import uk.gov.hmcts.reform.pip.publication.services.utils.SmokeTestBase;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.NotificationList;

import java.util.concurrent.ThreadLocalRandom;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.reform.pip.publication.services.utils.EmailNotificationClient.NOTIFICATION_TYPE;

@SpringBootTest(classes = {OAuthClient.class, EmailNotificationClient.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("smoke")
class SmokeTest extends SmokeTestBase {
    private static final String NOTIFY_URL = "/notify";
    private static final String MEDIA_WELCOME_EMAIL_URL = NOTIFY_URL + "/welcome-email";
    private static final String OTP_EMAIL_URL = NOTIFY_URL + "/otp";

    private static final String TEST_USER_EMAIL_PREFIX = String.format(
        "pip-ps-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999)
    );
    private static final String TEST_EMAIL = TEST_USER_EMAIL_PREFIX + "@justice.gov.uk";
    private static final String TEST_FULL_NAME = "test user";
    private static final String TEST_OTP = "OTP1234";

    private static final String STATUS_CODE_MATCH = "Status code does not match";
    private static final String RESPONSE_BODY_MATCH = "Response body does not match";
    private static final String EMAIL_ADDRESS_ERROR = "Email address does not match";

    @Autowired
    private EmailNotificationClient notificationClient;

    @Test
    void testHealthCheck() {
        Response response = doGetRequest("");

        assertThat(response.statusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(OK.value());

        assertThat(response.body().asString())
            .as(RESPONSE_BODY_MATCH)
            .isEqualTo("Welcome to PIP Publication Services");
    }

    @Test
    void testSendMediaAccountWelcomeEmail() throws NotificationClientException {
        WelcomeEmail requestBody = new WelcomeEmail(TEST_EMAIL, false, TEST_FULL_NAME);
        final Response response = doPostRequest(MEDIA_WELCOME_EMAIL_URL, requestBody, accessToken);

        assertThat(response.statusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(OK.value());

        Notification notification = extractNotification(response.getBody().asString());

        assertThat(notification.getEmailAddress())
            .as(EMAIL_ADDRESS_ERROR)
            .hasValue(TEST_EMAIL);
    }

    @Test
    void testSendOtpEmail() throws NotificationClientException {
        OtpEmail requestBody = new OtpEmail(TEST_OTP, TEST_EMAIL);

        final Response response = doPostRequest(OTP_EMAIL_URL, requestBody, b2cAccessToken);

        assertThat(response.getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(OK.value());

        Notification notification = extractNotification(response.getBody().asString());
        assertThat(notification.getEmailAddress())
            .as(EMAIL_ADDRESS_ERROR)
            .hasValue(TEST_EMAIL);
    }

    private Notification extractNotification(String referenceId) throws NotificationClientException {
        Awaitility.with()
            .pollInterval(1, SECONDS)
            .await()
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

        return notificationList.getNotifications().get(0);
    }
}
