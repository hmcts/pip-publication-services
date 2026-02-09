package uk.gov.hmcts.reform.pip.publication.services.controllers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.subscription.LegacyThirdPartySubscription;
import uk.gov.hmcts.reform.pip.model.subscription.LegacyThirdPartySubscriptionArtefact;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.model.subscription.LegacyThirdPartySubscription;
import uk.gov.hmcts.reform.pip.model.subscription.LegacyThirdPartySubscriptionArtefact;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.ChangeType;
import uk.gov.hmcts.reform.pip.model.system.admin.DeleteLocationAction;
import uk.gov.hmcts.reform.pip.model.system.admin.SystemAdminAction;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.BulkSubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaRejectionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.service.AwsS3Service;
import uk.gov.hmcts.reform.pip.publication.services.service.NotificationService;
import uk.gov.hmcts.reform.pip.publication.services.service.LegacyThirdPartyManagementService;
import uk.gov.hmcts.reform.pip.publication.services.service.UserNotificationService;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationControllerTest {

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
    private static final String HTML_FILE = "test.html";
    private static final String FORM_FILE_FIELD_NAME = "file";
    private static final String HTML_FILE_CONTENT_TYPE = "text/html";
    private static final String REFERENCE_ID = UUID.randomUUID().toString();
    private static final String MESSAGES_MATCH = "Messages should match";
    private static final String STATUS_CODES_MATCH = "Status codes should match";

    private WelcomeEmail validRequestBodyTrue;
    private List<MediaApplication> validMediaApplicationList;
    private SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
    private BulkSubscriptionEmail bulkSubscriptionEmail = new BulkSubscriptionEmail();
    private final List<NoMatchArtefact> noMatchArtefactList = new ArrayList<>();
    private DuplicatedMediaEmail createMediaSetupEmail;
    private LegacyThirdPartySubscription thirdPartySubscription = new LegacyThirdPartySubscription();
    private MediaVerificationEmail mediaVerificationEmail;
    private MediaRejectionEmail mediaRejectionEmail;
    private InactiveUserNotificationEmail inactiveUserNotificationEmail;
    private LegacyThirdPartySubscriptionArtefact thirdPartySubscriptionArtefact =
        new LegacyThirdPartySubscriptionArtefact();
    private SystemAdminAction systemAdminAction;
    private LocationSubscriptionDeletion locationSubscriptionDeletion = new LocationSubscriptionDeletion();

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserNotificationService userNotificationService;

    @Mock
    private LegacyThirdPartyManagementService legacyThirdPartyManagementService;

    @InjectMocks
    private NotificationController notificationController;

    @Mock
    private AwsS3Service awsS3Service;

    @BeforeEach
    void setup() {
        validRequestBodyTrue = new WelcomeEmail(VALID_EMAIL, TRUE_BOOL, FULL_NAME);
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

        when(legacyThirdPartyManagementService.handleThirdParty(thirdPartySubscription)).thenReturn(REFERENCE_ID);
        when(userNotificationService.mediaDuplicateUserEmailRequest(createMediaSetupEmail)).thenReturn(REFERENCE_ID);
        when(legacyThirdPartyManagementService.notifyThirdPartyForArtefactDeletion(thirdPartySubscriptionArtefact))
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

    @Test
    void testUploadHtmlFileWithHtmExtensionToAwsS3BucketReturnOkResponse() throws IOException {
        MultipartFile file = new MockMultipartFile(
            FORM_FILE_FIELD_NAME,
            "test.htm",
            HTML_FILE_CONTENT_TYPE,
            "<html><body>Test</body></html>".getBytes());

        doNothing().when(awsS3Service).uploadFile(anyString(), any(InputStream.class));

        ResponseEntity<String> response = notificationController.uploadHtmlToAwsS3Bucket(file);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODES_MATCH);
        assertEquals("File uploaded successfully to AWS S3 Bucket",
                     response.getBody(), "File upload message should match");
        verify(awsS3Service, times(1))
            .uploadFile(anyString(), any(InputStream.class));
    }

    @Test
    void testUploadHtmlToAwsS3BucketReturnOkResponse() throws IOException {
        MultipartFile file = new MockMultipartFile(
            FORM_FILE_FIELD_NAME,
            HTML_FILE,
            HTML_FILE_CONTENT_TYPE,
            "<html><body>Test</body></html>".getBytes());

        doNothing().when(awsS3Service).uploadFile(anyString(), any(InputStream.class));

        ResponseEntity<String> response = notificationController.uploadHtmlToAwsS3Bucket(file);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODES_MATCH);
        assertEquals("File uploaded successfully to AWS S3 Bucket",
                     response.getBody(), "File upload message should match");
        verify(awsS3Service, times(1))
            .uploadFile(anyString(), any(InputStream.class));
    }

    @Test
    void testUploadHtmlToAwsS3BucketReturnsBadRequestForEmptyFile() {
        MultipartFile emptyFile = new MockMultipartFile(
            FORM_FILE_FIELD_NAME,
            HTML_FILE,
            HTML_FILE_CONTENT_TYPE,
            new byte[0]);

        ResponseEntity<String> response = notificationController.uploadHtmlToAwsS3Bucket(emptyFile);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), STATUS_CODES_MATCH);
        assertEquals("File cannot be empty", response.getBody(),
                     "Error message should match");
    }

    @Test
    void testUploadHtmlToAwsS3BucketReturnsUnsupportedMediaType() {
        MultipartFile nonHtmlFile = new MockMultipartFile(
            FORM_FILE_FIELD_NAME,
            "test.pdf",
            HTML_FILE_CONTENT_TYPE,
            "Test".getBytes());

        ResponseEntity<String> response = notificationController.uploadHtmlToAwsS3Bucket(nonHtmlFile);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode(), STATUS_CODES_MATCH);
        assertEquals("Only HTM/HTML files are allowed", response.getBody(),
                     "Unsupported media type message should match");
    }

    @Test
    void testUploadHtmlToAwsS3BucketReturnsInternalServerErrorOnException()
        throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn(HTML_FILE);
        when(file.getInputStream()).thenThrow(new IOException("Stream failed"));

        ResponseEntity<String> response = notificationController.uploadHtmlToAwsS3Bucket(file);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), STATUS_CODES_MATCH);
        Assertions.assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().contains("Upload failed: Stream failed"),
                   "Internal server error message should contain exception details");
    }


}
