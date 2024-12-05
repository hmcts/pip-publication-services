package uk.gov.hmcts.reform.pip.publication.services.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
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
import uk.gov.hmcts.reform.pip.publication.services.models.request.BulkSubscriptionEmail;
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
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports", "PMD.TooManyFields", "PMD.CouplingBetweenObjects",
    "PMD.UseEnumCollections"})
class NotificationControllerTest extends RedisConfigurationTestBase {

    private static final String VALID_EMAIL = "test@email.com";
    private static final boolean TRUE_BOOL = true;
    private static final String TEST = "Test";
    private static final UUID ID = UUID.randomUUID();
    private static final String ID_STRING = UUID.randomUUID().toString();
    private static final String FULL_NAME = "Test user";
    private static final String REQUESTER_EMAIL = "test_user@justice.gov.uk";
    private static final String EMPLOYER = "Test employer";
    private static final String STATUS = "APPROVED";
    private static final LocalDateTime DATE_TIME = LocalDateTime.now();
    private static final String LAST_SIGNED_IN_DATE = "11 July 2022";
    private static final String IMAGE_NAME = "test-image.png";
    private static final String REFERENCE_ID = UUID.randomUUID().toString();
    private static final String MESSAGES_MATCH = "Messages should match";
    private static final String STATUS_CODES_MATCH = "Status codes should match";

    private WelcomeEmail validRequestBodyTrue;
    private List<MediaApplication> validMediaApplicationList;
    private SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
    private BulkSubscriptionEmail bulkSubscriptionEmail = new BulkSubscriptionEmail();
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

        subscriptionEmail.setEmail("a@b.com");
        subscriptionEmail.setSubscriptions(new HashMap<>());

        bulkSubscriptionEmail.setArtefactId(UUID.randomUUID());

        SubscriptionEmail subscriptionEmailForBulk = new SubscriptionEmail();
        subscriptionEmailForBulk.setEmail("a@b.com");
        subscriptionEmailForBulk.setSubscriptions(new HashMap<>());

        bulkSubscriptionEmail.setSubscriptionEmails(List.of(subscriptionEmailForBulk));

        createMediaSetupEmail = new DuplicatedMediaEmail();
        createMediaSetupEmail.setEmail("a@b.com");
        createMediaSetupEmail.setFullName("testName");

        systemAdminAction = new DeleteLocationAction();
        systemAdminAction.setRequesterEmail(REQUESTER_EMAIL);
        systemAdminAction.setEmailList(List.of(VALID_EMAIL));
        systemAdminAction.setChangeType(ChangeType.DELETE_LOCATION);
        systemAdminAction.setActionResult(ActionResult.ATTEMPTED);

        when(userNotificationService.mediaAccountWelcomeEmailRequest(validRequestBodyTrue)).thenReturn(REFERENCE_ID);
        when(notificationService.handleMediaApplicationReportingRequest(validMediaApplicationList))
            .thenReturn(REFERENCE_ID);

        noMatchArtefactList.add(new NoMatchArtefact(UUID.randomUUID(), "Test", "500"));
        noMatchArtefactList.add(new NoMatchArtefact(UUID.randomUUID(), "Test2", "123"));

        when(userNotificationService.adminAccountWelcomeEmailRequest(createdAdminWelcomeEmailValidBody))
            .thenReturn(REFERENCE_ID);
        when(thirdPartyManagementService.handleThirdParty(thirdPartySubscription)).thenReturn(REFERENCE_ID);
        when(userNotificationService.mediaDuplicateUserEmailRequest(createMediaSetupEmail)).thenReturn(REFERENCE_ID);
        when(thirdPartyManagementService.notifyThirdPartyForArtefactDeletion(thirdPartySubscriptionArtefact))
            .thenReturn(REFERENCE_ID);
        when(notificationService.handleMediaApplicationReportingRequest(validMediaApplicationList))
            .thenReturn(REFERENCE_ID);
        when(notificationService.unidentifiedBlobEmailRequest(noMatchArtefactList))
            .thenReturn(REFERENCE_ID);
        when(userNotificationService.mediaUserVerificationEmailRequest(mediaVerificationEmail))
            .thenReturn(REFERENCE_ID);
        when(userNotificationService.inactiveUserNotificationEmailRequest(inactiveUserNotificationEmail))
            .thenReturn(REFERENCE_ID);
        when(notificationService.sendSystemAdminUpdateEmailRequest(systemAdminAction)).thenReturn(REFERENCE_ID);
        when(notificationService.sendDeleteLocationSubscriptionEmail(locationSubscriptionDeletion))
            .thenReturn(REFERENCE_ID);
        when(notificationService.bulkSendSubscriptionEmail(bulkSubscriptionEmail)).thenReturn(REFERENCE_ID);
    }

    @Test
    void testValidBodyShouldReturnSuccessMessage() {
        assertEquals(REFERENCE_ID, notificationController.sendWelcomeEmail(validRequestBodyTrue).getBody(),
                     MESSAGES_MATCH);
    }

    @Test
    void testValidBodyShouldReturnOkResponse() {
        assertEquals(HttpStatus.OK, notificationController.sendWelcomeEmail(validRequestBodyTrue).getStatusCode(),
                     STATUS_CODES_MATCH
        );
    }

    @Test
    void testBulkSendSubscriptionReturnsAcceptedResponse() {
        ResponseEntity<String> responseEntity = notificationController.sendSubscriptionEmail(bulkSubscriptionEmail);

        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode(), STATUS_CODES_MATCH);
        assertEquals(REFERENCE_ID, responseEntity.getBody(), "Response body should contain accepted message");
    }

    @Test
    void testSendMediaReportingEmailReturnsSuccessMessage() {
        assertEquals(REFERENCE_ID, notificationController.sendMediaReportingEmail(validMediaApplicationList).getBody(),
                     MESSAGES_MATCH);
    }

    @Test
    void testSendAdminAccountWelcomeEmail() {
        assertEquals(REFERENCE_ID,
                     notificationController.sendAdminAccountWelcomeEmail(createdAdminWelcomeEmailValidBody).getBody(),
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
                       .contains(REFERENCE_ID), MESSAGES_MATCH);
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
        assertEquals(REFERENCE_ID, responseEntity.getBody(), "Response content does not contain the ID");
    }

    @Test
    void testSendUnidentifiedBlobEmailReturnsSuccessMessage() {
        assertEquals(REFERENCE_ID, notificationController.sendUnidentifiedBlobEmail(noMatchArtefactList).getBody(),
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
        assertEquals(REFERENCE_ID,
                     notificationController.notifyThirdPartyForArtefactDeletion(thirdPartySubscriptionArtefact)
                         .getBody(),
                     MESSAGES_MATCH);
    }

    @Test
    void testSendMediaVerificationEmailReturnsOk() {
        assertEquals(HttpStatus.OK, notificationController
                         .sendMediaUserVerificationEmail(mediaVerificationEmail).getStatusCode(),
                     STATUS_CODES_MATCH);
    }

    @Test
    void testSendMediaRejectionEmailReturnsOk() {
        assertEquals(HttpStatus.OK, notificationController
                         .sendMediaUserRejectionEmail(mediaRejectionEmail).getStatusCode(),
                     STATUS_CODES_MATCH);
    }

    @Test
    @SuppressWarnings("PMD.UnitTestAssertionsShouldIncludeMessage")
    void testSendInactiveUserNotificationEmailReturnsOk() {
        assertThat(notificationController.sendNotificationToInactiveUsers(inactiveUserNotificationEmail))
            .as("Response does not match")
            .extracting(
                ResponseEntity::getStatusCode,
                ResponseEntity::getBody
            )
            .containsExactly(
                HttpStatus.OK,
                REFERENCE_ID
            );
    }

    @Test
    @SuppressWarnings("PMD.UnitTestAssertionsShouldIncludeMessage")
    void testSendMiReportingEmailReturnsOk() {
        when(notificationService.handleMiDataForReporting()).thenReturn(REFERENCE_ID);

        assertThat(notificationController.sendMiReportingEmail())
            .as("Response does not match")
            .extracting(
                ResponseEntity::getStatusCode,
                ResponseEntity::getBody
            )
            .containsExactly(
                HttpStatus.OK,
                REFERENCE_ID
            );
    }

    @Test
    void testSendSystemAdminUpdateShouldReturnSuccessMessage() {
        assertEquals(REFERENCE_ID, notificationController.sendSystemAdminUpdate(systemAdminAction).getBody(),
                     MESSAGES_MATCH);
    }

    @Test
    void testSendSystemAdminUpdateShouldReturnOkResponse() {
        assertEquals(HttpStatus.OK, notificationController
                         .sendSystemAdminUpdate(systemAdminAction).getStatusCode(),
                     STATUS_CODES_MATCH);
    }

    @Test
    void testSendDeleteLocationSubscriptionEmailShouldReturnSuccessMessage() {
        assertEquals(REFERENCE_ID,
                     notificationController.sendDeleteLocationSubscriptionEmail(locationSubscriptionDeletion)
                         .getBody(),
                     MESSAGES_MATCH);
    }

    @Test
    void testSendDeleteLocationSubscriptionEmailShouldReturnOkResponse() {
        assertEquals(HttpStatus.OK, notificationController
                         .sendDeleteLocationSubscriptionEmail(locationSubscriptionDeletion).getStatusCode(),
                     STATUS_CODES_MATCH);
    }
}
