package uk.gov.hmcts.reform.pip.publication.services.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscriptionArtefact;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.ChangeType;
import uk.gov.hmcts.reform.pip.model.system.admin.DeleteLocationAction;
import uk.gov.hmcts.reform.pip.model.system.admin.SystemAdminAction;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaRejectionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.service.NotificationService;
import uk.gov.hmcts.reform.pip.publication.services.service.ThirdPartyManagementService;
import uk.gov.hmcts.reform.pip.publication.services.service.UserNotificationService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports", "PMD.TooManyFields"})
class NotificationControllerTest {

    private static final String VALID_EMAIL = "test@email.com";
    private static final boolean TRUE_BOOL = true;
    private static final String TEST = "Test";
    private static final UUID ID = UUID.randomUUID();
    private static final String ID_STRING = UUID.randomUUID().toString();
    private static final String FULL_NAME = "Test user";
    private static final String EMPLOYER = "Test employer";
    private static final String STATUS = "APPROVED";
    private static final LocalDateTime DATE_TIME = LocalDateTime.now();
    private static final String LAST_SIGNED_IN_DATE = "11 July 2022";
    private static final String IMAGE_NAME = "test-image.png";
    private static final String SUCCESS_ID = "SuccessId";
    private static final String MESSAGES_MATCH = "Messages should match";
    private static final String STATUS_CODES_MATCH = "Status codes should match";

    private WelcomeEmail validRequestBodyTrue;
    private List<MediaApplication> validMediaApplicationList;
    private SubscriptionEmail subscriptionEmail;
    private final List<NoMatchArtefact> noMatchArtefactList = new ArrayList<>();
    private CreatedAdminWelcomeEmail createdAdminWelcomeEmailValidBody;
    private DuplicatedMediaEmail createMediaSetupEmail;
    private ThirdPartySubscription thirdPartySubscription = new ThirdPartySubscription();
    private MediaVerificationEmail mediaVerificationEmail;
    private MediaRejectionEmail mediaRejectionEmail;
    private InactiveUserNotificationEmail inactiveUserNotificationEmail;
    private ThirdPartySubscriptionArtefact thirdPartySubscriptionArtefact = new ThirdPartySubscriptionArtefact();
    private SystemAdminAction systemAdminAction;
    private LocationSubscriptionDeletion locationSubscriptionDeletion = new LocationSubscriptionDeletion();

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserNotificationService userNotificationService;

    @Mock
    private ThirdPartyManagementService thirdPartyManagementService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setup() {
        validRequestBodyTrue = new WelcomeEmail(VALID_EMAIL, TRUE_BOOL, FULL_NAME);
        createdAdminWelcomeEmailValidBody = new CreatedAdminWelcomeEmail(VALID_EMAIL, TEST, TEST);
        thirdPartySubscription.setApiDestination(TEST);
        thirdPartySubscription.setArtefactId(ID);
        thirdPartySubscriptionArtefact.setApiDestination(TEST);
        thirdPartySubscriptionArtefact.setArtefact(new Artefact());
        validMediaApplicationList = List.of(new MediaApplication(ID, FULL_NAME,
                                                                 VALID_EMAIL, EMPLOYER,
                                                                 ID_STRING, IMAGE_NAME,
                                                                 DATE_TIME, STATUS, DATE_TIME));
        mediaVerificationEmail = new MediaVerificationEmail(FULL_NAME, VALID_EMAIL);
        mediaRejectionEmail = new MediaRejectionEmail(FULL_NAME, VALID_EMAIL, new HashMap<>());
        inactiveUserNotificationEmail = new InactiveUserNotificationEmail(FULL_NAME, VALID_EMAIL,
                                                                          "PI_AAD", LAST_SIGNED_IN_DATE);

        subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail("a@b.com");
        subscriptionEmail.setArtefactId(UUID.randomUUID());
        subscriptionEmail.setSubscriptions(new HashMap<>());

        createMediaSetupEmail = new DuplicatedMediaEmail();
        createMediaSetupEmail.setEmail("a@b.com");
        createMediaSetupEmail.setFullName("testName");

        systemAdminAction = new DeleteLocationAction();
        systemAdminAction.setRequesterName(FULL_NAME);
        systemAdminAction.setEmailList(List.of(VALID_EMAIL));
        systemAdminAction.setChangeType(ChangeType.DELETE_LOCATION);
        systemAdminAction.setActionResult(ActionResult.ATTEMPTED);

        when(userNotificationService.handleWelcomeEmailRequest(validRequestBodyTrue)).thenReturn(SUCCESS_ID);
        when(notificationService.subscriptionEmailRequest(subscriptionEmail)).thenReturn(SUCCESS_ID);
        when(notificationService.handleMediaApplicationReportingRequest(validMediaApplicationList))
            .thenReturn(SUCCESS_ID);

        noMatchArtefactList.add(new NoMatchArtefact(UUID.randomUUID(), "Test", "500"));
        noMatchArtefactList.add(new NoMatchArtefact(UUID.randomUUID(), "Test2", "123"));

        when(userNotificationService.azureNewUserEmailRequest(createdAdminWelcomeEmailValidBody))
            .thenReturn(SUCCESS_ID);
        when(thirdPartyManagementService.handleThirdParty(thirdPartySubscription)).thenReturn(SUCCESS_ID);
        when(userNotificationService.mediaDuplicateUserEmailRequest(createMediaSetupEmail)).thenReturn(SUCCESS_ID);
        when(thirdPartyManagementService.notifyThirdPartyForArtefactDeletion(thirdPartySubscriptionArtefact))
            .thenReturn(SUCCESS_ID);
        when(notificationService.handleMediaApplicationReportingRequest(validMediaApplicationList))
            .thenReturn(SUCCESS_ID);
        when(notificationService.unidentifiedBlobEmailRequest(noMatchArtefactList))
            .thenReturn(SUCCESS_ID);
        when(userNotificationService.mediaUserVerificationEmailRequest(mediaVerificationEmail))
            .thenReturn(SUCCESS_ID);
        when(userNotificationService.inactiveUserNotificationEmailRequest(inactiveUserNotificationEmail))
            .thenReturn(SUCCESS_ID);
        when(notificationService.sendSystemAdminUpdateEmailRequest(systemAdminAction)).thenReturn(List.of());
    }

    @Test
    void testValidBodyShouldReturnSuccessMessage() {
        assertTrue(
            notificationController.sendWelcomeEmail(validRequestBodyTrue).getBody()
                .contains("Welcome email successfully sent with referenceId SuccessId"),
            MESSAGES_MATCH
        );
    }

    @Test
    void testValidBodyShouldReturnOkResponse() {
        assertEquals(HttpStatus.OK, notificationController.sendWelcomeEmail(validRequestBodyTrue).getStatusCode(),
                     STATUS_CODES_MATCH
        );
    }

    @Test
    void testSendSubscriptionReturnsOkResponse() {
        ResponseEntity<String> responseEntity = notificationController.sendSubscriptionEmail(subscriptionEmail);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), STATUS_CODES_MATCH);
        assertTrue(Objects.requireNonNull(responseEntity.getBody()).contains(SUCCESS_ID),
                   "Response content does not contain the ID");
    }

    @Test
    void testSendMediaReportingEmailReturnsSuccessMessage() {
        assertTrue(
            notificationController.sendMediaReportingEmail(validMediaApplicationList).getBody()
                .contains("Media applications report email sent successfully with referenceId SuccessId"),
            MESSAGES_MATCH);
    }

    @Test
    void testSendAdminAccountWelcomeEmail() {
        assertTrue(notificationController.sendAdminAccountWelcomeEmail(createdAdminWelcomeEmailValidBody).getBody()
                       .contains("Created admin welcome email successfully sent with referenceId SuccessId"),
                   MESSAGES_MATCH);
    }

    @Test
    void testSendAdminAccountWelcomeEmailReturnsOk() {
        assertEquals(HttpStatus.OK, notificationController
                         .sendAdminAccountWelcomeEmail(createdAdminWelcomeEmailValidBody).getStatusCode(),
                     STATUS_CODES_MATCH);
    }

    @Test
    void testSendThirdPartySubscription() {
        assertTrue(notificationController.sendThirdPartySubscription(thirdPartySubscription).getBody()
                       .contains(SUCCESS_ID), MESSAGES_MATCH);
    }

    @Test
    void testSendThirdPartySubscriptionReturnsOk() {
        assertEquals(HttpStatus.OK, notificationController.sendThirdPartySubscription(thirdPartySubscription)
            .getStatusCode(), STATUS_CODES_MATCH);
    }

    @Test
    void testSendMediaReportingEmailReturnsOkResponse() {
        assertEquals(HttpStatus.OK, notificationController.sendMediaReportingEmail(
            validMediaApplicationList).getStatusCode(), STATUS_CODES_MATCH);
    }

    @Test
    void testSendDuplicateMediaAccountEmailReturnsOkResponse() {
        ResponseEntity<String> responseEntity = notificationController
            .sendDuplicateMediaAccountEmail(createMediaSetupEmail);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), STATUS_CODES_MATCH);
        assertTrue(Objects.requireNonNull(responseEntity.getBody()).contains(SUCCESS_ID),
                   "Response content does not contain the ID");
    }

    @Test
    void testSendUnidentifiedBlobEmailReturnsSuccessMessage() {
        assertTrue(
            notificationController.sendUnidentifiedBlobEmail(noMatchArtefactList).getBody()
                .contains("Unidentified blob email successfully sent with reference id: SuccessId"),
            MESSAGES_MATCH);
    }

    @Test
    void testSendUnidentifiedBlobEmailReturnsOkResponse() {
        assertEquals(HttpStatus.OK, notificationController
            .sendUnidentifiedBlobEmail(noMatchArtefactList).getStatusCode(),
                     STATUS_CODES_MATCH);
    }

    @Test
    void testSendThirdPartySubscriptionEmptyListReturnsOk() {
        assertEquals(HttpStatus.OK,
                     notificationController.notifyThirdPartyForArtefactDeletion(thirdPartySubscriptionArtefact)
                         .getStatusCode(),
                     STATUS_CODES_MATCH);
    }

    @Test
    void testSendThirdPartySubscriptionEmptyList() {
        assertTrue(notificationController.notifyThirdPartyForArtefactDeletion(thirdPartySubscriptionArtefact).getBody()
                       .contains(SUCCESS_ID), MESSAGES_MATCH);
    }

    @Test
    void testSendMediaVerificationEmailReturnsOk() {
        assertEquals(HttpStatus.OK, notificationController
                         .sendMediaUserVerificationEmail(mediaVerificationEmail).getStatusCode(),
                     STATUS_CODES_MATCH);
    }

    @Test
    void testSendMediaRejectionEmailReturnsOk() throws IOException {
        assertEquals(HttpStatus.OK, notificationController
                         .sendMediaUserRejectionEmail(mediaRejectionEmail).getStatusCode(),
                     STATUS_CODES_MATCH);
    }

    @Test
    @SuppressWarnings("PMD.JUnitAssertionsShouldIncludeMessage")
    void testSendInactiveUserNotificationEmailReturnsOk() {
        assertThat(notificationController.sendNotificationToInactiveUsers(inactiveUserNotificationEmail))
            .as("Response does not match")
            .extracting(
                ResponseEntity::getStatusCode,
                ResponseEntity::getBody
            )
            .contains(
                HttpStatus.OK,
                "Inactive user sign-in notification email successfully sent with referenceId: SuccessId"
            );
    }

    @Test
    @SuppressWarnings("PMD.JUnitAssertionsShouldIncludeMessage")
    void testSendMiReportingEmailReturnsOk() {
        when(notificationService.handleMiDataForReporting()).thenReturn(SUCCESS_ID);

        assertThat(notificationController.sendMiReportingEmail())
            .as("Response does not match")
            .extracting(
                ResponseEntity::getStatusCode,
                ResponseEntity::getBody
            )
            .contains(
                HttpStatus.OK,
                "MI data reporting email successfully sent with referenceId: " + SUCCESS_ID
            );
    }

    @Test
    void testSendSystemAdminUpdateShouldReturnSuccessMessage() {
        assertTrue(
            notificationController.sendSystemAdminUpdate(systemAdminAction).getBody()
                .contains("Send notification email successfully to all system admin with referenceId"),
            MESSAGES_MATCH
        );
    }

    @Test
    void testSendSystemAdminUpdateShouldReturnOkResponse() {
        assertEquals(HttpStatus.OK, notificationController
                         .sendSystemAdminUpdate(systemAdminAction).getStatusCode(),
                     STATUS_CODES_MATCH
        );
    }

    @Test
    void testSendDeleteLocationSubscriptionEmailShouldReturnSuccessMessage() {
        assertTrue(
            notificationController.sendDeleteLocationSubscriptionEmail(locationSubscriptionDeletion).getBody()
                .contains("Location subscription email successfully sent with reference id"),
            MESSAGES_MATCH
        );
    }

    @Test
    void testSendDeleteLocationSubscriptionEmailShouldReturnOkResponse() {
        assertEquals(HttpStatus.OK, notificationController
                         .sendDeleteLocationSubscriptionEmail(locationSubscriptionDeletion).getStatusCode(),
                     STATUS_CODES_MATCH
        );
    }
}
