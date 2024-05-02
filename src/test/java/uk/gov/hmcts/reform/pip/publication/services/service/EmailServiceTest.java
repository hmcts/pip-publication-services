package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.TooManyEmailsException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL;

@SpringBootTest
@ActiveProfiles("test")
class EmailServiceTest extends RedisConfigurationTestBase {

    private static final String EMAIL = "test@email.com";
    private static final String ERROR_MESSAGE = "Test message";
    private static final Map<String, Object> PERSONALISATION = Map.of("Value", "OtherValue");
    private static final TooManyEmailsException TOO_MANY_EMAILS_EXCEPTION = new TooManyEmailsException(ERROR_MESSAGE);

    @Autowired
    private EmailService emailService;

    @MockBean
    private EmailClient emailClient;

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Test
    void testHandleEmailGeneration() {

    }

    @Test
    void testHandleBatchEmailGeneration() {

    }

    @Test
    void testSendEmailWithSuccess() throws NotificationClientException {
        EmailToSend emailToSend = new EmailToSend(EMAIL, MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.getTemplate(),
                                                  PERSONALISATION, UUID.randomUUID().toString());

        when(emailClient.sendEmail(MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.getTemplate(), EMAIL, PERSONALISATION,
                                   emailToSend.getReferenceId()))
            .thenReturn(sendEmailResponse);

        assertThat(emailService.sendEmail(emailToSend))
            .as("Email response does not match expected response")
            .isEqualTo(sendEmailResponse);
    }

    @Test
    void testSendEmailWithFailure() throws NotificationClientException {
        String exceptionMessage = "This is an exception";
        EmailToSend emailToSend = new EmailToSend(EMAIL, MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.getTemplate(),
                                                  PERSONALISATION, UUID.randomUUID().toString());

        when(emailClient.sendEmail(MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.getTemplate(), EMAIL, PERSONALISATION,
                                   emailToSend.getReferenceId()))
            .thenThrow(new NotificationClientException(exceptionMessage));

        assertThatThrownBy(() -> emailService.sendEmail(emailToSend))
            .as("Exception response does not match")
            .isInstanceOf(NotifyException.class)
            .hasMessage(exceptionMessage);
    }
}
