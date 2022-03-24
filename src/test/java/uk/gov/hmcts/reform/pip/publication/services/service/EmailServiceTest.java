package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class EmailServiceTest {

    private static final String EMAIL = "test@email.com";
    private static final String INVALID_EMAIL = "invalid";

    @Autowired
    private EmailService emailService;

    @MockBean
    private EmailClient emailClient;

    private SendEmailResponse sendEmailResponse;

    @BeforeEach
    void setup() throws NotificationClientException {
        sendEmailResponse = mock(SendEmailResponse.class);

        when(emailClient.sendEmail(eq(Templates.ADMIN_ACCOUNT_CREATION_EMAIL.template), eq(INVALID_EMAIL), anyMap(),
                                   anyString()
        ))
            .thenThrow(NotificationClientException.class);

        when(emailClient.sendEmail(
            eq(Templates.NEW_USER_WELCOME_EMAIL.template),
            eq(INVALID_EMAIL),
            anyMap(),
            anyString()
        ))
            .thenThrow(NotificationClientException.class);

        when(emailClient.sendEmail(eq(Templates.EXISTING_USER_WELCOME_EMAIL.template), eq(INVALID_EMAIL), anyMap(),
                                   anyString()
        ))
            .thenThrow(NotificationClientException.class);

        when(emailClient.sendEmail(eq(Templates.NEW_USER_WELCOME_EMAIL.template), eq(EMAIL), anyMap(),
                                   anyString()
        ))
            .thenReturn(sendEmailResponse);

        when(emailClient.sendEmail(eq(Templates.EXISTING_USER_WELCOME_EMAIL.template), eq(EMAIL), anyMap(),
                                   anyString()
        ))
            .thenReturn(sendEmailResponse);

        when(emailClient.sendEmail(eq(Templates.ADMIN_ACCOUNT_CREATION_EMAIL.template), eq(EMAIL), anyMap(),
                                   anyString()
        ))
            .thenReturn(sendEmailResponse);
    }

    @Test
    void buildAadEmailReturnsSuccess() {
        EmailToSend aadEmail = emailService.buildCreatedAdminWelcomeEmail(new CreatedAdminWelcomeEmail(
            EMAIL,
            "b",
            "c"
        ));
        assertEquals(sendEmailResponse, emailService.sendEmail(aadEmail),
                     "Should return a SendEmailResponse"
        );
    }

    @Test
    void existingUserWelcomeValidEmailReturnsSuccess() {
        EmailToSend welcomeEmail =
            emailService.buildWelcomeEmail(
                new WelcomeEmail(EMAIL, true),
                Templates.EXISTING_USER_WELCOME_EMAIL.template
            );
        assertEquals(sendEmailResponse, emailService.sendEmail(welcomeEmail),
                     "Should return a SendEmailResponse"
        );
    }

    @Test
    void newUserWelcomeValidEmailReturnsSuccess() {
        EmailToSend welcomeEmail =
            emailService.buildWelcomeEmail(new WelcomeEmail(EMAIL, false), Templates.NEW_USER_WELCOME_EMAIL.template);
        assertEquals(sendEmailResponse, emailService.sendEmail(welcomeEmail),
                     "Should return a SendEmailResponse"
        );
    }

    @Test
    void existingUserWelcomeInvalidEmailException() {
        EmailToSend welcomeEmail = emailService.buildWelcomeEmail(
            new WelcomeEmail(INVALID_EMAIL, true),
            Templates.EXISTING_USER_WELCOME_EMAIL.template
        );
        assertThrows(NotifyException.class, () -> emailService.sendEmail(welcomeEmail));
    }

    @Test
    void newUserWelcomeInvalidEmailException() {
        EmailToSend welcomeEmail = emailService.buildWelcomeEmail(
            new WelcomeEmail(INVALID_EMAIL, false),
            Templates.NEW_USER_WELCOME_EMAIL.template
        );
        assertThrows(NotifyException.class, () -> emailService.sendEmail(welcomeEmail));
    }

    @Test
    void newAadUserInvalidEmailException() {
        EmailToSend aadEmail = emailService.buildCreatedAdminWelcomeEmail(
            new CreatedAdminWelcomeEmail(INVALID_EMAIL, "b", "c"));
        assertThrows(NotifyException.class, () -> emailService.sendEmail(aadEmail));
    }
}
