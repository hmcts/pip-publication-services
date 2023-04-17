package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaRejectionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
import uk.gov.service.notify.SendEmailResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class UserNotificationServiceTest {

    private static final String REJECTION_EMAIL_FIRST_LINE_JSON = "\"id\":\"123e4567-e89b-12d3-a456-426614174000\",";
    private static final String EMAIL = "test@email.com";
    private final Map<String, Object> personalisationMap = Map.ofEntries(
        entry("email", VALID_BODY_AAD.getEmail()),
        entry("surname", VALID_BODY_AAD.getSurname()),
        entry("first_name", VALID_BODY_AAD.getForename()),
        entry("reset_password_link", "http://www.test.com"),
        entry("sign_in_page_link", "http://www.google.com"),
        entry("media_sign_in_link", "http://www.google.com")
    );

    private Map<String, List<String>> testReasons;

    private static final String FULL_NAME = "fullName";
    private static final String LAST_SIGNED_IN_DATE = "11 July 2022";
    private static final WelcomeEmail VALID_BODY_EXISTING = new WelcomeEmail(
        EMAIL, true, FULL_NAME);
    private static final WelcomeEmail VALID_BODY_NEW = new WelcomeEmail(
        EMAIL, false, FULL_NAME);
    private static final CreatedAdminWelcomeEmail VALID_BODY_AAD = new CreatedAdminWelcomeEmail(
        EMAIL, "test_forename", "test_surname");

    private static final MediaVerificationEmail MEDIA_VERIFICATION_EMAIL = new MediaVerificationEmail(
        EMAIL, FULL_NAME);

    private static final InactiveUserNotificationEmail INACTIVE_USER_NOTIFICATION_EMAIL_AAD =
        new InactiveUserNotificationEmail(EMAIL, FULL_NAME, "PI_AAD", LAST_SIGNED_IN_DATE);

    private static final InactiveUserNotificationEmail INACTIVE_USER_NOTIFICATION_EMAIL_CFT =
        new InactiveUserNotificationEmail(EMAIL, FULL_NAME, "CFT_IDAM", LAST_SIGNED_IN_DATE);

    private static final String SUCCESS_REF_ID = "successRefId";

    private final EmailToSend validEmailBodyForEmailClient = new EmailToSend(
        VALID_BODY_NEW.getEmail(),
        Templates.BAD_BLOB_EMAIL.template,
        personalisationMap,
        SUCCESS_REF_ID
    );
    private static final Integer LOCATION_ID = 1;
    private static final String LOCATION_NAME = "Location Name";


    private final EmailToSend validEmailBodyForDuplicateMediaUserClient =
        new EmailToSend(VALID_BODY_NEW.getEmail(), Templates.MEDIA_DUPLICATE_ACCOUNT_EMAIL.template,
                        personalisationMap, SUCCESS_REF_ID
        );

    private static final String EXISTING_REFERENCE_ID =
        "Existing user with valid JSON should return successful referenceId.";
    private final Location location = new Location();

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Autowired
    private UserNotificationService userNotificationService;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    void setup() {
        testReasons = new ConcurrentHashMap<>();
        testReasons.put("Reason 1", List.of("Reason short description",  "Reason long description"));

        when(sendEmailResponse.getReference()).thenReturn(Optional.of(SUCCESS_REF_ID));
        when(emailService.sendEmail(validEmailBodyForEmailClient)).thenReturn(sendEmailResponse);
        when(emailService.sendEmail(validEmailBodyForDuplicateMediaUserClient)).thenReturn(sendEmailResponse);

        location.setLocationId(LOCATION_ID);
        location.setName(LOCATION_NAME);
    }

    @Test
    void testValidPayloadReturnsSuccessExisting() {
        when(emailService.buildWelcomeEmail(VALID_BODY_EXISTING, Templates.EXISTING_USER_WELCOME_EMAIL.template))
            .thenReturn(validEmailBodyForEmailClient);
        assertEquals(SUCCESS_REF_ID, userNotificationService.handleWelcomeEmailRequest(VALID_BODY_EXISTING),
                     EXISTING_REFERENCE_ID
        );
    }

    @Test
    void testValidPayloadReturnsSuccessNew() {
        when(emailService.buildWelcomeEmail(VALID_BODY_NEW, Templates.MEDIA_NEW_ACCOUNT_SETUP.template))
            .thenReturn(validEmailBodyForEmailClient);
        assertEquals(SUCCESS_REF_ID, userNotificationService.handleWelcomeEmailRequest(VALID_BODY_NEW),
                     EXISTING_REFERENCE_ID
        );
    }

    @Test
    void testValidPayloadReturnsSuccessAzure() {
        when(emailService.buildCreatedAdminWelcomeEmail(
            VALID_BODY_AAD,
            Templates.ADMIN_ACCOUNT_CREATION_EMAIL.template
        ))
            .thenReturn(validEmailBodyForEmailClient);
        assertEquals(SUCCESS_REF_ID, userNotificationService.azureNewUserEmailRequest(VALID_BODY_AAD),
                     "Azure user with valid JSON should return successful referenceId."
        );
    }

    @Test
    void testValidPayloadReturnsSuccessDuplicateMediaAccount() {
        DuplicatedMediaEmail createMediaSetupEmail = new DuplicatedMediaEmail();
        createMediaSetupEmail.setFullName("test_forename");
        createMediaSetupEmail.setEmail(EMAIL);

        when(emailService.buildDuplicateMediaSetupEmail(
            createMediaSetupEmail,
            Templates.MEDIA_DUPLICATE_ACCOUNT_EMAIL.template
        ))
            .thenReturn(validEmailBodyForDuplicateMediaUserClient);
        assertEquals(SUCCESS_REF_ID, userNotificationService.mediaDuplicateUserEmailRequest(createMediaSetupEmail),
                     EXISTING_REFERENCE_ID
        );
    }

    @Test
    void testValidPayloadReturnsSuccessMediaVerification() {
        when(emailService.buildMediaUserVerificationEmail(
            MEDIA_VERIFICATION_EMAIL,
            Templates.MEDIA_USER_VERIFICATION_EMAIL.template
        ))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(
            SUCCESS_REF_ID,
            userNotificationService.mediaUserVerificationEmailRequest(MEDIA_VERIFICATION_EMAIL),
            "Media user verification email successfully sent with referenceId: referenceId."
        );
    }

    @Test
    void testValidPayloadReturnsSuccessInactiveUserNotificationForAad() {
        when(emailService.buildInactiveUserNotificationEmail(
            INACTIVE_USER_NOTIFICATION_EMAIL_AAD,
            Templates.INACTIVE_USER_NOTIFICATION_EMAIL_AAD.template
        ))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, userNotificationService.inactiveUserNotificationEmailRequest(
                         INACTIVE_USER_NOTIFICATION_EMAIL_AAD),
                     "Inactive user notification should return successful reference ID"
        );
    }

    @Test
    void testValidPayloadReturnsSuccessInactiveUserNotificationForCft() {
        when(emailService.buildInactiveUserNotificationEmail(
            INACTIVE_USER_NOTIFICATION_EMAIL_CFT,
            Templates.INACTIVE_USER_NOTIFICATION_EMAIL_CFT.template
        ))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, userNotificationService.inactiveUserNotificationEmailRequest(
                         INACTIVE_USER_NOTIFICATION_EMAIL_CFT),
                     "Inactive user notification should return successful reference ID"
        );
    }

    @Test
    void testMediaUserRejectionEmailRequestWithValidData() throws IOException {
        MediaRejectionEmail mediaRejectionEmail = new MediaRejectionEmail(
            "Test Name",
            EMAIL,
            testReasons
        );
        EmailToSend expectedEmail = new EmailToSend(EMAIL, Templates.MEDIA_USER_REJECTION_EMAIL.template,
                                                    new HashMap<>(), "123e4567-e89b-12d3-a456-426614174000"
        );
        String jsonResponse = "{"
            + REJECTION_EMAIL_FIRST_LINE_JSON
            + "\"reference\":\"123e4567-e89b-12d3-a456-426614174000\","
            + "\"content\":{"
            + "\"body\":\"Email body\","
            + "\"subject\":\"Email subject\","
            + "\"from_email\":\"from@email.com\""
            + "},"
            + "\"template\":{"
            + REJECTION_EMAIL_FIRST_LINE_JSON
            + "\"version\":1,"
            + "\"uri\":\"https://example.com/template_uri\""
            + "}"
            + "}";
        SendEmailResponse sendEmailResponse = new SendEmailResponse(jsonResponse);

        when(emailService.buildMediaApplicationRejectionEmail(any(MediaRejectionEmail.class), any(String.class)))
            .thenReturn(expectedEmail);
        when(emailService.sendEmail(expectedEmail)).thenReturn(sendEmailResponse);

        String result = userNotificationService.mediaUserRejectionEmailRequest(mediaRejectionEmail);

        assertEquals("123e4567-e89b-12d3-a456-426614174000", result, "Reference ID should match the expected value");
    }

    @Test
    void testMediaUserRejectionEmailRequestWithNullReference() throws IOException {
        MediaRejectionEmail mediaRejectionEmail = new MediaRejectionEmail(
            "Test Name",
            EMAIL,
            testReasons
        );
        EmailToSend expectedEmail = new EmailToSend(EMAIL, Templates.MEDIA_USER_REJECTION_EMAIL.template,
                                                    new HashMap<>(), "123e4567-e89b-12d3-a456-426614174000"
        );
        String jsonResponse =
            "{"
                + REJECTION_EMAIL_FIRST_LINE_JSON
                + "\"content\":{"
                + "\"body\":\"Email body\","
                + "\"subject\":\"Email subject\","
                + "\"from_email\":\"from@email.com\""
                + "},"
                + "\"template\":{"
                + REJECTION_EMAIL_FIRST_LINE_JSON
                + "\"version\":1,"
                + "\"uri\":\"https://example.com/template_uri\""
                + "}"
                + "}";
        SendEmailResponse sendEmailResponse = new SendEmailResponse(jsonResponse);

        when(emailService.buildMediaApplicationRejectionEmail(any(MediaRejectionEmail.class), any(String.class)))
            .thenReturn(expectedEmail);
        when(emailService.sendEmail(expectedEmail)).thenReturn(sendEmailResponse);

        String result = userNotificationService.mediaUserRejectionEmailRequest(mediaRejectionEmail);

        Assertions.assertNull(result, "expected null");
    }


}
