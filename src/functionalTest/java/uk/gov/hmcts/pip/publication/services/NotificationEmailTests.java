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
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.ChangeType;
import uk.gov.hmcts.reform.pip.model.system.admin.CreateSystemAdminAction;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaRejectionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.NotificationList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.pip.publication.services.utils.EmailNotificationClient.NOTIFICATION_TYPE;
import static uk.gov.hmcts.reform.pip.model.system.admin.ActionResult.ATTEMPTED;
import static uk.gov.hmcts.reform.pip.model.system.admin.ChangeType.ADD_USER;

@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class, EmailNotificationClient.class})
@SuppressWarnings({"PMD.TooManyMethods"})
class NotificationEmailTests extends FunctionalTestBase {
    private static final String NOTIFY_URL = "/notify";
    private static final String MEDIA_WELCOME_EMAIL_URL = NOTIFY_URL + "/welcome-email";
    private static final String MEDIA_VERIFICATION_EMAIL_URL = NOTIFY_URL + "/media/verification";
    private static final String INACTIVE_USER_NOTIFICATION_EMAIL_URL = NOTIFY_URL + "/user/sign-in";

    private static final String DUPLICATE_MEDIA_USER_EMAIL_URL = NOTIFY_URL + "/duplicate/media";
    private static final String REJECTED_MEDIA_ACCOUNT_EMAIL_URL = NOTIFY_URL + "/media/reject";
    private static final String SYSADMIN_UPDATE_EMAIL_URL = NOTIFY_URL + "/sysadmin/update";

    private static final String TEST_USER_EMAIL_PREFIX = String.format(
        "pip-ps-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));
    private static final String TEST_EMAIL = TEST_USER_EMAIL_PREFIX + "@justice.gov.uk";
    private static final String LAST_SIGNED_IN_DATE = "2023-12-12 11:49:28.532005";
    private static final String TEST_INVALID_EMAIL = "test_user";
    private static final String TEST_FULL_NAME = "test user";
    private static final ActionResult TEST_ACTION = ATTEMPTED;
    private static final ChangeType TEST_CHANGE_TYPE = ADD_USER;

    private static final String EMAIL_ADDRESS_ERROR = "Email address does not match";
    private static final String EMAIL_SUBJECT_ERROR = "Email subject does not match";
    private static final String EMAIL_NAME_ERROR = "Name in email body does not match";
    private static final String EMAIL_BODY_ERROR = "Email body does not match";

    @Autowired
    private EmailNotificationClient notificationClient;

    private Notification extractNotification(Response response, String email) throws NotificationClientException {
        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        String referenceId = response.getBody().asString();
        assertThat(referenceId)
            .isNotEmpty();

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

        Notification notification = notificationList.getNotifications().get(0);

        assertThat(notification.getEmailAddress())
            .as(EMAIL_ADDRESS_ERROR)
            .hasValue(email);

        return notification;
    }

    @Test
    void shouldSendMediaWelcomeEmailToNewUser() throws NotificationClientException {
        Map<Object, Object> requestBody = new ConcurrentHashMap<>();
        requestBody.put("email", TEST_EMAIL);
        requestBody.put("fullName", TEST_FULL_NAME);
        requestBody.put("isExisting", false);

        final Response response = doPostRequest(
            MEDIA_WELCOME_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

        Notification notification = extractNotification(response, TEST_EMAIL);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue("Court and tribunal hearings account – request approved");

        assertThat(notification.getBody())
            .as(EMAIL_NAME_ERROR)
            .contains(TEST_FULL_NAME);

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("Click on the link to confirm your email address and finish creating your account");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("p=B2C_1A_PASSWORD_RESET");
    }

    @Test
    void shouldSendMediaWelcomeEmailToExistingUser() throws NotificationClientException {
        Map<Object, Object> requestBody = new ConcurrentHashMap<>();
        requestBody.put("email", TEST_EMAIL);
        requestBody.put("fullName", TEST_FULL_NAME);
        requestBody.put("isExisting", true);

        final Response response = doPostRequest(
            MEDIA_WELCOME_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

        Notification notification = extractNotification(response, TEST_EMAIL);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue("Court and tribunal hearings service – Welcome");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("We have received your email address as an existing recipient of non-public hearing lists.");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("Click on the link to confirm your email address and finish creating your account");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("p=B2C_1A_PASSWORD_RESET");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("/subscription-management");
    }

    @Test
    void shouldSendMediaVerificationEmailToInactiveUser() throws NotificationClientException {
        MediaVerificationEmail requestBody = new MediaVerificationEmail(TEST_FULL_NAME, TEST_EMAIL);

        final Response response = doPostRequest(
            MEDIA_VERIFICATION_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

        Notification notification = extractNotification(response, TEST_EMAIL);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue("Court and tribunal hearings account – annual email verification");

        assertThat(notification.getBody())
            .as(EMAIL_NAME_ERROR)
            .contains(TEST_FULL_NAME);

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains(
                "Click on the link and login to confirm your email address and that you still have "
                    + "legitimate reasons to access information not open to the public.");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("/media-verification?p=B2C_1_SignInMediaVerification");
    }

    @Test
    void shouldSendNotificationEmailToInactiveCftUser() throws NotificationClientException {
        InactiveUserNotificationEmail requestBody = new InactiveUserNotificationEmail(TEST_EMAIL, TEST_FULL_NAME,
                                                                                      UserProvenances.CFT_IDAM.name(),
                                                                                      LAST_SIGNED_IN_DATE
        );

        final Response response = doPostRequest(
            INACTIVE_USER_NOTIFICATION_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

        Notification notification = extractNotification(response, TEST_EMAIL);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue("Court and Tribunal Hearings Service – MyHMCTS Account");

        assertThat(notification.getBody())
            .as(EMAIL_NAME_ERROR)
            .contains(TEST_FULL_NAME);

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("Please click on the link below and sign in to prevent your details "
                          + "for the Court and tribunal hearings service being deleted");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains(LAST_SIGNED_IN_DATE);

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("/cft-login");
    }

    @Test
    void shouldSendDuplicateEmailNotification() throws NotificationClientException {
        DuplicatedMediaEmail requestBody = new DuplicatedMediaEmail();
        requestBody.setEmail(TEST_EMAIL);
        requestBody.setFullName(TEST_FULL_NAME);

        final Response response = doPostRequest(
            DUPLICATE_MEDIA_USER_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

        Notification notification = extractNotification(response, TEST_EMAIL);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue("Your account already exists");

        assertThat(notification.getBody())
            .as(EMAIL_NAME_ERROR)
            .contains(TEST_FULL_NAME);

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("We are unable to create a new Court and Tribunal Hearings account "
                          + "because your email address already exists.");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("/login?p=B2C_1_SignInUserFlow");
    }

    @Test
    void shouldSendMediaAccountRejectionEmail() throws NotificationClientException {
        Map<String, List<String>> reasonsMap = new ConcurrentHashMap<>();
        reasonsMap.put("Key A", List.of("Reason 1", "Reason 2"));
        reasonsMap.put("Key B", List.of("Reason 3", "Reason 4"));

        MediaRejectionEmail mediaRejectionEmail = new MediaRejectionEmail(
            TEST_FULL_NAME,
            TEST_EMAIL,
            reasonsMap
        );

        final Response response = doPostRequest(
            REJECTED_MEDIA_ACCOUNT_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            mediaRejectionEmail
        );

        Notification notification = extractNotification(response, TEST_EMAIL);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue("Your request for a court and tribunal hearings account has been rejected.");

        assertThat(notification.getBody())
            .as(EMAIL_NAME_ERROR)
            .contains(TEST_FULL_NAME);

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("Your request for a court and tribunal hearings "
                          + "account has been rejected for the following reason(s):");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .matches(body -> Stream.of("Reason 1", "Reason 2", "Reason 3", "Reason 4")
                .allMatch(body::contains));
    }

    @Test
    void shouldSendUpdateEmailToSysAdminUser() throws NotificationClientException {
        CreateSystemAdminAction systemAdminAction = createSystemAdminUpdateAction();

        final Response response = doPostRequest(
            SYSADMIN_UPDATE_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            systemAdminAction
        );

        Notification notification = extractNotification(response, TEST_EMAIL);

        assertThat(notification.getSubject().get())
            .as(EMAIL_SUBJECT_ERROR)
            .contains("Reportable action – Add User");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains(String.format("Please be aware that %s has attempted to execute a Add User change", TEST_EMAIL));
    }

    @Test
    void shouldReturnBadPayloadExceptionErrorMessageWhenTargetSystemAdminEmailIsInvalid() {
        CreateSystemAdminAction systemAdminAction = createSystemAdminUpdateAction();
        systemAdminAction.setEmailList(List.of(TEST_INVALID_EMAIL));

        final Response response = doPostRequest(
            SYSADMIN_UPDATE_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            systemAdminAction
        );

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST.value());

        String responseBody = response.getBody().asString();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody).contains("Not a valid email address");
    }

    private CreateSystemAdminAction createSystemAdminUpdateAction() {
        CreateSystemAdminAction systemAdminAction = new CreateSystemAdminAction();
        systemAdminAction.setAccountEmail(TEST_EMAIL);
        systemAdminAction.setEmailList(List.of(TEST_EMAIL));
        systemAdminAction.setRequesterEmail(TEST_EMAIL);
        systemAdminAction.setActionResult(TEST_ACTION);
        systemAdminAction.setChangeType(TEST_CHANGE_TYPE);
        return systemAdminAction;
    }
}
