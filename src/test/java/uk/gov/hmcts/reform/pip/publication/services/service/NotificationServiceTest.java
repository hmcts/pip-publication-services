package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailResponseHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
public class NotificationServiceTest {

    private static final String VALID_EMAIL = "test@email.com";

    private static final WelcomeEmail VALID_BODY_EXISTING = new WelcomeEmail(
        "test@email.com", true);
    private static final WelcomeEmail VALID_BODY_NEW = new WelcomeEmail(
        "test@email.com", false);
    public static final String SUCCESS_REF_ID = "successRefId";

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    public void setup() {

        when(emailService.buildEmail(VALID_EMAIL, Templates.EXISTING_USER_WELCOME_EMAIL.template))
            .thenReturn(EmailResponseHelper.stubSendEmailResponseWithReferenceID(SUCCESS_REF_ID));
        when(emailService.buildEmail(VALID_EMAIL, Templates.NEW_USER_WELCOME_EMAIL.template))
            .thenReturn(EmailResponseHelper.stubSendEmailResponseWithReferenceID(SUCCESS_REF_ID));
    }

    @Test
    public void testValidPayloadReturnsSuccessExisting() {
        assertEquals(SUCCESS_REF_ID, notificationService.handleWelcomeEmailRequest(VALID_BODY_EXISTING),
                     "Existing user with valid JSON should return successful referenceId"
        );
    }

    @Test
    public void testValidPayloadReturnsSuccessNew() {
        assertEquals(SUCCESS_REF_ID, notificationService.handleWelcomeEmailRequest(VALID_BODY_NEW),
                     "Existing user with valid JSON should return successful referenceId"
        );
    }
}
