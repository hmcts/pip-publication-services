package uk.gov.hmcts.reform.pip.publication.services.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
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

@SpringBootTest
class NotificationControllerTest {

    private static final String VALID_EMAIL = "test@email.com";
    private static final boolean TRUE_BOOL = true;
    private static final UUID ID = UUID.randomUUID();
    private static final String ID_STRING = UUID.randomUUID().toString();
    private static final String FULL_NAME = "Test user";
    private static final String EMPLOYER = "Test employer";
    private static final String STATUS = "APPROVED";
    private static final LocalDateTime DATE_TIME = LocalDateTime.now();
    private static final String IMAGE_NAME = "test-image.png";
    private static final String SUCCESS_ID = "successId";

    private WelcomeEmail validRequestBodyTrue;
    private List<MediaApplication> validMediaApplicationList;
    private SubscriptionEmail subscriptionEmail;
    private final Map<String, String> testUnidentifiedBlobMap = new ConcurrentHashMap<>();

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setup() {
        validRequestBodyTrue = new WelcomeEmail(VALID_EMAIL, TRUE_BOOL);
        validMediaApplicationList = List.of(new MediaApplication(ID, FULL_NAME,
            VALID_EMAIL, EMPLOYER, ID_STRING, IMAGE_NAME, DATE_TIME, STATUS, DATE_TIME));

        subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail("a@b.com");
        subscriptionEmail.setArtefactId(UUID.randomUUID());
        subscriptionEmail.setSubscriptions(new HashMap<>());

        testUnidentifiedBlobMap.put("Test", "500");
        testUnidentifiedBlobMap.put("Test2", "123");

        when(notificationService.handleWelcomeEmailRequest(validRequestBodyTrue)).thenReturn(SUCCESS_ID);
        when(notificationService.subscriptionEmailRequest(subscriptionEmail)).thenReturn(SUCCESS_ID);
        when(notificationService.handleMediaApplicationReportingRequest(validMediaApplicationList))
            .thenReturn(SUCCESS_ID);
        when(notificationService.unidentifiedBlobEmailRequest(testUnidentifiedBlobMap))
            .thenReturn(SUCCESS_ID);
    }

    @Test
    void testValidBodyShouldReturnSuccessMessage() {
        assertTrue(
            notificationController.sendWelcomeEmail(validRequestBodyTrue).getBody()
                .contains("Welcome email successfully sent with referenceId successId"),
            "Messages should match"
        );
    }

    @Test
    void testValidBodyShouldReturnOkResponse() {
        assertEquals(HttpStatus.OK, notificationController.sendWelcomeEmail(validRequestBodyTrue).getStatusCode(),
                     "Status codes should match"
        );
    }

    @Test
    void testSendSubscriptionReturnsOkResponse() {
        ResponseEntity<String> responseEntity = notificationController.sendSubscriptionEmail(subscriptionEmail);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "Status codes should match");
        assertTrue(Objects.requireNonNull(responseEntity.getBody()).contains("successId"),
                   "Response content does not contain the ID");
    }

    @Test
    void testSendMediaReportingEmailReturnsSuccessMessage() {
        assertTrue(
            notificationController.sendMediaReportingEmail(validMediaApplicationList).getBody()
                .contains("Media applications report email sent successfully with referenceId successId"),
            "Messages should match"
        );
    }

    @Test
    void testSendMediaReportingEmailReturnsOkResponse() {
        assertEquals(HttpStatus.OK, notificationController.sendMediaReportingEmail(
            validMediaApplicationList).getStatusCode(), "Status codes should match");
    }

    @Test
    void testSendUnidentifiedBlobEmailReturnsSuccessMessage() {
        assertTrue(
            notificationController.sendUnidentifiedBlobEmail(testUnidentifiedBlobMap).getBody()
                .contains("Unidentified blob email successfully sent with reference id: successId"),
            "Messages should match"
        );
    }

    @Test
    void testSendUnidentifiedBlobEmailReturnsOkResponse() {
        assertEquals(HttpStatus.OK, notificationController
            .sendUnidentifiedBlobEmail(testUnidentifiedBlobMap).getStatusCode(),
                     "status codes should match");
    }
}
