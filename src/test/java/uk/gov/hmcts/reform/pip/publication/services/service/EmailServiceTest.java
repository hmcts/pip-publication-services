package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
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
public class EmailServiceTest {

    private static final String EMAIL = "test@email.com";
    private static final String TEMPLATE = "template";
    private static final String INVALID_EMAIL = "invalid";

    @Autowired
    private EmailService emailService;

    @MockBean
    private EmailClient emailClient;

    private SendEmailResponse sendEmailResponse;

    @BeforeEach
    public void setup() throws NotificationClientException {
        sendEmailResponse = mock(SendEmailResponse.class);

        when(emailClient.sendEmail(eq(TEMPLATE), eq(EMAIL), anyMap(), anyString()))
            .thenReturn(sendEmailResponse);

        when(emailClient.sendEmail(eq(TEMPLATE), eq(INVALID_EMAIL), anyMap(), anyString()))
            .thenThrow(NotificationClientException.class);
    }

    @Test
    public void testBuildEmailReturnsSuccess() {
        assertEquals(sendEmailResponse, emailService.buildEmail(EMAIL, TEMPLATE), "Should return a SendEmailResponse");
    }

    @Test
    public void testFailedEmailReturnsNotifyException() {
        assertThrows(NotifyException.class, () -> {
            emailService.buildEmail(INVALID_EMAIL, TEMPLATE);
        });
    }
}
