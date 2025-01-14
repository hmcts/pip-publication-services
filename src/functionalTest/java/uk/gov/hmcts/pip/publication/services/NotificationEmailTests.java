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
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.ChangeType;
import uk.gov.hmcts.reform.pip.model.system.admin.CreateSystemAdminAction;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.NotificationList;

import java.util.List;
import java.util.Map;

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
class NotificationEmailTests extends FunctionalTestBase {
    private static final String NOTIFY_URL = "/notify";
    private static final String MEDIA_WELCOME_EMAIL_URL = NOTIFY_URL + "/welcome-email";
    private static final String MEDIA_VERIFICATION_EMAIL_URL = NOTIFY_URL + "/media/verification";
    private static final String INACTIVE_USER_NOTIFICATION_EMAIL_URL = NOTIFY_URL + "/user/sign-in";
    private static final String LOCATION_SUBSCRIPTION_EMAIL_URL = NOTIFY_URL + "/location-subscription-delete";
    private static final String SYSADMIN_UPDATE_EMAIL_URL = NOTIFY_URL + "/sysadmin/update";

    private static final String TEST_EMAIL = "test_user@justice.gov.uk";
    private static final String TEST_INVALID_EMAIL = "test_user";
    private static final String TEST_FULL_NAME = "test user";
    private static final String TEST_LOCATION = "test location";
    private static final ActionResult TEST_ACTION = ATTEMPTED;
    private static final ChangeType TEST_CHANGE_TYPE = ADD_USER;

    private static final String EMAIL_ADDRESS_ERROR = "Email address does not match";
    private static final String EMAIL_SUBJECT_ERROR = "Email subject does not match";
    private static final String EMAIL_BODY_ERROR = "Email body does not match";

    @Autowired
    private EmailNotificationClient notificationClient;

    @Test
    void shouldSendMediaWelcomeEmailToNewUser() throws NotificationClientException {
        WelcomeEmail requestBody = new WelcomeEmail(TEST_EMAIL, false, TEST_FULL_NAME);

        final Response response = doPostRequest(
            MEDIA_WELCOME_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

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
            .hasValue(TEST_EMAIL);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue("Court and tribunal hearings account – request approved");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("Click on the link to confirm your email address and finish creating your account");
    }

    @Test
    void shouldSendMediaVerificationEmailToInactiveUser() throws NotificationClientException {
        MediaVerificationEmail requestBody = new MediaVerificationEmail(TEST_FULL_NAME, TEST_EMAIL);

        final Response response = doPostRequest(
            MEDIA_VERIFICATION_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

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
            .hasValue(TEST_EMAIL);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue("Court and tribunal hearings account – annual email verification");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains(
                "Click on the link and login to confirm your email address and that you still have "
                    + "legitimate reasons to access information not open to the public.");
    }

    @Test
    void shouldSendNotificationEmailToInactiveAdminUser() throws NotificationClientException {

        InactiveUserNotificationEmail requestBody = new InactiveUserNotificationEmail(TEST_EMAIL, TEST_FULL_NAME,
                                                                                      UserProvenances.PI_AAD.name(),
                                                                            "2023-12-12 11:49:28.532005"
        );

        final Response response = doPostRequest(
            INACTIVE_USER_NOTIFICATION_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

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
            .hasValue(TEST_EMAIL);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue("Court and tribunal hearings account – activate account");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("Click on the link to prevent your Court and tribunal hearings account being deleted");
    }

    @Test
    void shouldSendNotificationEmailToInactiveCftUser() throws NotificationClientException {
        InactiveUserNotificationEmail requestBody = new InactiveUserNotificationEmail(TEST_EMAIL, TEST_FULL_NAME,
                                                                                      UserProvenances.CFT_IDAM.name(),
                                                                             "2023-12-12 11:49:28.532005"
        );

        final Response response = doPostRequest(
            INACTIVE_USER_NOTIFICATION_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

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
            .hasValue(TEST_EMAIL);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue("Court and Tribunal Hearings Service – MyHMCTS Account");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("Please click on the link below and sign in to prevent your details "
                          + "for the Court and tribunal hearings service being deleted");
    }

    @Test
    void shouldSendLocationSubscriptionDeletionEmailToUser() throws NotificationClientException {
        LocationSubscriptionDeletion requestBody = new LocationSubscriptionDeletion(TEST_LOCATION,
                                                                                    List.of(TEST_EMAIL, TEST_EMAIL));

        final Response response = doPostRequest(
            LOCATION_SUBSCRIPTION_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

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
                    && notificationList.getNotifications().size() == 2;
            });

        NotificationList notificationList = notificationClient.getNotifications(
            null, NOTIFICATION_TYPE, referenceId, null
        );

        Notification notification = notificationList.getNotifications().getFirst();

        assertThat(notification.getEmailAddress())
            .as(EMAIL_ADDRESS_ERROR)
            .hasValue(TEST_EMAIL);
    }

    @Test
    void shouldReturnNotifyExceptionErrorMessageWhenEmailIsInvalid() {
        LocationSubscriptionDeletion requestBody = new LocationSubscriptionDeletion(TEST_LOCATION,
                                                                                    List.of(TEST_INVALID_EMAIL));

        final Response response = doPostRequest(
            LOCATION_SUBSCRIPTION_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST.value());

        String responseBody = response.getBody().asString();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody).contains("Not a valid email address");
    }

    @Test
    void shouldSendUpdateEmailToSysAdminUser() throws NotificationClientException {
        CreateSystemAdminAction systemAdminAction = createSystemAdminUpdateAction();

        final Response response = doPostRequest(
            SYSADMIN_UPDATE_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            systemAdminAction
        );

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
                    && notificationList.getNotifications().size() == 2;
            });

        NotificationList notificationList = notificationClient.getNotifications(
            null, NOTIFICATION_TYPE, referenceId, null
        );

        Notification notification = notificationList.getNotifications().getFirst();

        assertThat(notification.getEmailAddress())
            .as(EMAIL_ADDRESS_ERROR)
            .hasValue(TEST_EMAIL);
    }

    @Test
    void shouldReturnNotifyExceptionErrorMessageWhenTargetSystemAdminEmailIsInvalid() {
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
