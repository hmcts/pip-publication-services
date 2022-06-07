package uk.gov.hmcts.reform.pip.publication.services.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.ThirdPartySubscription;
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
    private static final boolean TRUE_BOOL = true;
    private static final String TEST = "Test";
    private static final UUID TEST_ID = UUID.randomUUID();
    private static final String SUCCESS = "SuccessId";
    private static final String MESSAGES_MATCH = "Messages should match";
    private static final String STATUS_CODES_MATCH = "Status codes should match";

    private WelcomeEmail validRequestBodyTrue;
    private SubscriptionEmail subscriptionEmail;
    private CreatedAdminWelcomeEmail createdAdminWelcomeEmailValidBody;
    private ThirdPartySubscription thirdPartySubscription = new ThirdPartySubscription();

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setup() {
        validRequestBodyTrue = new WelcomeEmail(VALID_EMAIL, TRUE_BOOL);
        createdAdminWelcomeEmailValidBody = new CreatedAdminWelcomeEmail(VALID_EMAIL, TEST, TEST);
        thirdPartySubscription.setApiDestination(TEST);
        thirdPartySubscription.setArtefactId(TEST_ID);

        subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail("a@b.com");
        subscriptionEmail.setArtefactId(UUID.randomUUID());
        subscriptionEmail.setSubscriptions(new HashMap<>());

        when(notificationService.handleWelcomeEmailRequest(validRequestBodyTrue)).thenReturn("successId");
        when(notificationService.subscriptionEmailRequest(subscriptionEmail)).thenReturn("successId");
        when(notificationService.handleWelcomeEmailRequest(validRequestBodyTrue)).thenReturn(SUCCESS);
        when(notificationService.azureNewUserEmailRequest(createdAdminWelcomeEmailValidBody)).thenReturn(SUCCESS);
        when(notificationService.handleThirdParty(thirdPartySubscription)).thenReturn(SUCCESS);
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

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "Status codes should match");
        assertTrue(Objects.requireNonNull(responseEntity.getBody()).contains("successId"),
                   "Response content does not contain the ID");
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
                       .contains(SUCCESS), MESSAGES_MATCH);
    }

    @Test
    void testSendThirdPartySubscriptionReturnsOk() {
        assertEquals(HttpStatus.OK, notificationController.sendThirdPartySubscription(thirdPartySubscription)
            .getStatusCode(), STATUS_CODES_MATCH);
    }
}
