//package uk.gov.hmcts.reform.pip.publication.services.service;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mock;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
//import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
//import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
//import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
//import uk.gov.service.notify.SendEmailResponse;
//
//import java.util.Map;
//import java.util.Optional;
//
//import static java.util.Map.entry;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.Mockito.when;
//
//@SpringBootTest
//class NotificationServiceTest {
//    private final Map<String, String> personalisationMap = Map.ofEntries(
//        entry("email", VALID_BODY_AAD.getEmail()),
//        entry("surname", VALID_BODY_AAD.getSurname()),
//        entry("first_name", VALID_BODY_AAD.getForename()),
//        entry("reset_password_link", "http://www.test.com"),
//        entry("sign_in_page_link", "http://www.google.com")
//    );
//
//    private static final WelcomeEmail VALID_BODY_EXISTING = new WelcomeEmail(
//        "test@email.com", true);
//    private static final WelcomeEmail VALID_BODY_NEW = new WelcomeEmail(
//        "test@email.com", false);
//    private static final CreatedAdminWelcomeEmail VALID_BODY_AAD = new CreatedAdminWelcomeEmail(
//        "test@email.com", "test_forename", "test_surname");
//    static final String SUCCESS_REF_ID = "successRefId";
//    private final EmailToSend validEmailBodyForEmailClient = new EmailToSend(VALID_BODY_NEW.getEmail(),
//                                                                             Templates.NEW_USER_WELCOME_EMAIL.template,
//                                                                             personalisationMap,
//                                                                             SUCCESS_REF_ID
//    );
//
//    @Mock
//    private SendEmailResponse sendEmailResponse;
//
//    @Autowired
//    private NotificationService notificationService;
//
//    @MockBean
//    private EmailService emailService;
//
//    @BeforeEach
//    void setup() {
//        when(sendEmailResponse.getReference()).thenReturn(Optional.of(SUCCESS_REF_ID));
//        when(emailService.sendEmail(validEmailBodyForEmailClient)).thenReturn(sendEmailResponse);
//    }
//
//    @Test
//    void testValidPayloadReturnsSuccessExisting() {
//        when(emailService.buildWelcomeEmail(VALID_BODY_EXISTING, Templates.EXISTING_USER_WELCOME_EMAIL.template))
//            .thenReturn(validEmailBodyForEmailClient);
//        assertEquals(SUCCESS_REF_ID, notificationService.handleWelcomeEmailRequest(VALID_BODY_EXISTING),
//                     "Existing user with valid JSON should return successful referenceId."
//        );
//    }
//
//    @Test
//    void testValidPayloadReturnsSuccessNew() {
//        when(emailService.buildWelcomeEmail(VALID_BODY_NEW, Templates.NEW_USER_WELCOME_EMAIL.template))
//            .thenReturn(validEmailBodyForEmailClient);
//        assertEquals(SUCCESS_REF_ID, notificationService.handleWelcomeEmailRequest(VALID_BODY_NEW),
//                     "Existing user with valid JSON should return successful referenceId."
//        );
//    }
//
//    @Test
//    void testValidPayloadReturnsSuccessAzure() {
//        when(emailService.buildCreatedAdminWelcomeEmail(VALID_BODY_AAD,
//                                                        Templates.ADMIN_ACCOUNT_CREATION_EMAIL.template))
//            .thenReturn(validEmailBodyForEmailClient);
//        assertEquals(SUCCESS_REF_ID, notificationService.azureNewUserEmailRequest(VALID_BODY_AAD),
//                     "Azure user with valid JSON should return successful referenceId.");
//    }
//}
