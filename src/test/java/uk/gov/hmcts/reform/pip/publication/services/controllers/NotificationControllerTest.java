package uk.gov.hmcts.reform.pip.publication.services.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.publication.services.models.request.ThirdPartySubscriptionArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.service.NotificationService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SuppressWarnings({"PMD.TooManyMethods"})
@SpringBootTest
@ActiveProfiles("test")
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
    private static final String IMAGE_NAME = "test-image.png";
    private static final String SUCCESS_ID = "SuccessId";
    private static final String MESSAGES_MATCH = "Messages should match";
    private static final String STATUS_CODES_MATCH = "Status codes should match";

    private WelcomeEmail validRequestBodyTrue;
    private List<MediaApplication> validMediaApplicationList;
    private SubscriptionEmail subscriptionEmail;
    private final Map<String, String> testUnidentifiedBlobMap = new ConcurrentHashMap<>();
    private CreatedAdminWelcomeEmail createdAdminWelcomeEmailValidBody;
    private DuplicatedMediaEmail createMediaSetupEmail;
    private ThirdPartySubscription thirdPartySubscription = new ThirdPartySubscription();
    private MediaVerificationEmail mediaVerificationEmail;
    private ThirdPartySubscriptionArtefact thirdPartySubscriptionArtefact = new ThirdPartySubscriptionArtefact();

    @Mock
    private NotificationService notificationService;

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

        subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail("a@b.com");
        subscriptionEmail.setArtefactId(UUID.randomUUID());
        subscriptionEmail.setSubscriptions(new HashMap<>());

        createMediaSetupEmail = new DuplicatedMediaEmail();
        createMediaSetupEmail.setEmail("a@b.com");
        createMediaSetupEmail.setFullName("testName");


        when(notificationService.handleWelcomeEmailRequest(validRequestBodyTrue)).thenReturn(SUCCESS_ID);
        when(notificationService.subscriptionEmailRequest(subscriptionEmail)).thenReturn(SUCCESS_ID);
        when(notificationService.handleMediaApplicationReportingRequest(validMediaApplicationList))
            .thenReturn(SUCCESS_ID);

        testUnidentifiedBlobMap.put("Test", "500");
        testUnidentifiedBlobMap.put("Test2", "123");

        when(notificationService.azureNewUserEmailRequest(createdAdminWelcomeEmailValidBody)).thenReturn(SUCCESS_ID);
        when(notificationService.handleThirdParty(thirdPartySubscription)).thenReturn(SUCCESS_ID);
        when(notificationService.mediaDuplicateUserEmailRequest(createMediaSetupEmail)).thenReturn(SUCCESS_ID);
        when(notificationService.handleThirdParty(thirdPartySubscriptionArtefact)).thenReturn(SUCCESS_ID);
        when(notificationService.handleMediaApplicationReportingRequest(validMediaApplicationList))
            .thenReturn(SUCCESS_ID);
        when(notificationService.unidentifiedBlobEmailRequest(testUnidentifiedBlobMap))
            .thenReturn(SUCCESS_ID);
        when(notificationService.mediaUserVerificationEmailRequest(mediaVerificationEmail))
            .thenReturn(SUCCESS_ID);
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
            notificationController.sendUnidentifiedBlobEmail(testUnidentifiedBlobMap).getBody()
                .contains("Unidentified blob email successfully sent with reference id: SuccessId"),
            MESSAGES_MATCH);
    }

    @Test
    void testSendUnidentifiedBlobEmailReturnsOkResponse() {
        assertEquals(HttpStatus.OK, notificationController
            .sendUnidentifiedBlobEmail(testUnidentifiedBlobMap).getStatusCode(),
                     STATUS_CODES_MATCH);
    }

    @Test
    void testSendThirdPartySubscriptionEmptyListReturnsOk() {
        assertEquals(HttpStatus.OK,
                     notificationController.sendThirdPartySubscription(thirdPartySubscriptionArtefact).getStatusCode(),
                     STATUS_CODES_MATCH);
    }

    @Test
    void testSendThirdPartySubscriptionEmptyList() {
        assertTrue(notificationController.sendThirdPartySubscription(thirdPartySubscriptionArtefact).getBody()
                       .contains(SUCCESS_ID), MESSAGES_MATCH);
    }

    @Test
    void testSendMediaVerificationEmailReturnsOk() {
        assertEquals(HttpStatus.OK, notificationController
                         .sendMediaUserVerificationEmail(mediaVerificationEmail).getStatusCode(),
                     STATUS_CODES_MATCH);
    }
}
