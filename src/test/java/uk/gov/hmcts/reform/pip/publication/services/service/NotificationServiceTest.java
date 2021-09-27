package uk.gov.hmcts.reform.pip.publication.services.service;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.BadPayloadException;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailResponseHelper;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest
public class NotificationServiceTest {

    private static final String VALID_EMAIL = "test@email.com";

    private static final JSONObject VALID_BODY_EXISTING = new JSONObject(
        "{email: 'test@email.com', isExisting: 'true'}");
    private static final JSONObject VALID_BODY_NEW = new JSONObject(
        "{email: 'test@email.com', isExisting: 'false'}");
    private static final JSONObject INVALID_BODY_NO_EMAIL_KEY = new JSONObject(
        "{isExisting: 'true'}");
    private static final JSONObject INVALID_BODY_NO_BOOL_KEY = new JSONObject(
        "{email: 'test@email.com'}");
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

    @Test
    public void testMissingEmailKeyReturnsBadPayloadException() {
        BadPayloadException ex = assertThrows(BadPayloadException.class, () -> {
            notificationService.handleWelcomeEmailRequest(INVALID_BODY_NO_EMAIL_KEY);
        });
        assertEquals("email was not found in the json payload", ex.getMessage(), "Error messages should match");
    }

    @Test
    public void testMissingExistingKeyReturnsBadPayloadException() {
        BadPayloadException ex = assertThrows(BadPayloadException.class, () -> {
            notificationService.handleWelcomeEmailRequest(INVALID_BODY_NO_BOOL_KEY);
        });
        assertEquals("isExisting was not found in the json payload", ex.getMessage(), "Error messages should match");
    }

    @Test
    public void testAllMissingKeysReturnsBadPayloadException() {
        JSONObject missingPayload = new JSONObject("{}");
        assertThrows(BadPayloadException.class, () -> notificationService.handleWelcomeEmailRequest(missingPayload));
    }
}
