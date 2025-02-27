package uk.gov.hmcts.pip.publication.services;

import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.pip.publication.services.utils.EmailNotificationClient;
import uk.gov.hmcts.pip.publication.services.utils.FunctionalTestBase;
import uk.gov.hmcts.pip.publication.services.utils.OAuthClient;
import uk.gov.hmcts.reform.pip.publication.services.models.request.OtpEmail;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.NotificationList;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.pip.publication.services.utils.EmailNotificationClient.NOTIFICATION_TYPE;

@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class, EmailNotificationClient.class})
class B2cNotificationEmailTests extends FunctionalTestBase {
    private static final String NOTIFY_URL = "/notify";
    private static final String OTP_EMAIL_URL = NOTIFY_URL + "/otp";

    private static final String TEST_USER_EMAIL_PREFIX = String.format(
        "pip-ps-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));
    private static final String TEST_EMAIL = TEST_USER_EMAIL_PREFIX + "@justice.gov.uk";
    private static final String TEST_OTP = "OTP1234";
    private static final String TEST_INVALID_EMAIL = "test";

    private static final String EMAIL_ADDRESS_ERROR = "Email address does not match";
    private static final String EMAIL_SUBJECT_ERROR = "Email subject does not match";
    private static final String EMAIL_BODY_ERROR = "Email body does not match";

    @Autowired
    private EmailNotificationClient notificationClient;

    @Autowired
    private OAuthClient authClient;

    protected String b2cBearerToken;

    @BeforeEach
    void setUp() {
        b2cBearerToken = authClient.generateB2cBearerToken();
    }

    @Test
    void shouldSendOtpEmail() throws NotificationClientException {
        OtpEmail requestBody = new OtpEmail(TEST_OTP, TEST_EMAIL);

        final Response response = doPostRequest(
            OTP_EMAIL_URL,
            Map.of(AUTHORIZATION, b2cBearerToken),
            requestBody
        );

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
                    && notificationList.getNotifications().size() == 1;
            });

        NotificationList notificationList = notificationClient.getNotifications(
            null, NOTIFICATION_TYPE, referenceId, null
        );

        Notification notification = notificationList.getNotifications().get(0);

        assertThat(notification.getEmailAddress())
            .as(EMAIL_ADDRESS_ERROR)
            .hasValue(TEST_EMAIL);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue("Court and tribunal hearings service – verification code");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("This is your verification code from court and tribunal hearings service. "
                          + "This code will expire after 10 minutes.");
    }

    @Test
    void shouldReturnNotifyExceptionErrorMessageWithInvalidEmail() {
        OtpEmail requestBody = new OtpEmail(TEST_OTP, TEST_INVALID_EMAIL);

        final Response response = doPostRequest(
            OTP_EMAIL_URL,
            Map.of(AUTHORIZATION, b2cBearerToken),
            requestBody
        );

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST.value());

        String responseMessage = response.getBody().asString();
        assertThat(responseMessage).isNotEmpty();
        assertThat(responseMessage).contains("must be a well-formed email address");
    }

    @Test
    void shouldReturnForbiddenWhenUserNotAuthorized() {
        OtpEmail requestBody = new OtpEmail(TEST_OTP, TEST_EMAIL);

        final Response response = doPostRequest(
            OTP_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN.value());
    }
}
