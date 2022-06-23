package uk.gov.hmcts.reform.pip.publication.services.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.service.NotificationService;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
class NotificationControllerTest {

    private static final String VALID_EMAIL = "test@email.com";
    private static final String FULL_NAME = "fullName";
    private static final boolean TRUE_BOOL = true;
    private static final String SUCCESS_ID = "successId";
    private static final String STATUS_CODES_SHOULD_MATCH = "Status codes should match";

    private WelcomeEmail validRequestBodyTrue;
    private SubscriptionEmail subscriptionEmail;
    private DuplicatedMediaEmail createMediaSetupEmail;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setup() {
        validRequestBodyTrue = new WelcomeEmail(VALID_EMAIL, TRUE_BOOL, FULL_NAME);

        subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail("a@b.com");
        subscriptionEmail.setArtefactId(UUID.randomUUID());
        subscriptionEmail.setSubscriptions(new HashMap<>());

        createMediaSetupEmail = new DuplicatedMediaEmail();
        createMediaSetupEmail.setEmail("a@b.com");
        createMediaSetupEmail.setFullName("testName");

        when(notificationService.handleWelcomeEmailRequest(validRequestBodyTrue)).thenReturn(SUCCESS_ID);
        when(notificationService.subscriptionEmailRequest(subscriptionEmail)).thenReturn(SUCCESS_ID);
        when(notificationService.mediaDuplicateUserEmailRequest(createMediaSetupEmail)).thenReturn(SUCCESS_ID);
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
                     STATUS_CODES_SHOULD_MATCH
        );
    }

    @Test
    void testSendSubscriptionReturnsOkResponse() {
        ResponseEntity<String> responseEntity = notificationController.sendSubscriptionEmail(subscriptionEmail);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), STATUS_CODES_SHOULD_MATCH);
        assertTrue(Objects.requireNonNull(responseEntity.getBody()).contains(SUCCESS_ID),
                   "Response content does not contain the ID");
    }

    @Test
    void testSendDuplicateMediaAccountEmailReturnsOkResponse() {
        ResponseEntity<String> responseEntity = notificationController
            .sendDuplicateMediaAccountEmail(createMediaSetupEmail);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), STATUS_CODES_SHOULD_MATCH);
        assertTrue(Objects.requireNonNull(responseEntity.getBody()).contains(SUCCESS_ID),
                   "Response content does not contain the ID");
    }
}
