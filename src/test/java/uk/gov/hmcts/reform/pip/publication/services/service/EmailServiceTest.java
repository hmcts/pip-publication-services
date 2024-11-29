package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.TooManyEmailsException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.LocationSubscriptionDeletionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.AdminWelcomeEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.ADMIN_ACCOUNT_CREATION_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.DELETE_LOCATION_SUBSCRIPTION;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
class EmailServiceTest extends RedisConfigurationTestBase {

    private static final String EMAIL = "test@email.com";
    private static final String EMAIL1 = "test1@testing.com";
    private static final String EMAIL2 = "test2@testing.com";
    private static final String EMAIL3 = "test3@testing.com";
    private static final String FORENAME = "Forename";
    private static final String SURNAME = "Surname";
    private static final String LOCATION_NAME = "Location name";
    private static final String REFERENCE_ID = UUID.randomUUID().toString();

    private static final String EMAIL_MESSAGE = "Email address does not match";
    private static final String TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String ERROR_MESSAGE = "Test message";
    private static final Map<String, Object> PERSONALISATION = Map.of("Value", "OtherValue");

    private static final TooManyEmailsException TOO_MANY_EMAILS_EXCEPTION = new TooManyEmailsException(ERROR_MESSAGE);

    @Autowired
    private EmailService emailService;

    @MockBean
    private EmailClient emailClient;

    @MockBean
    private RateLimitingService rateLimitingService;

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Test
    void testHandleEmailGenerationWithinRateLimit() {
        CreatedAdminWelcomeEmail welcomeEmail = new CreatedAdminWelcomeEmail(EMAIL, FORENAME, SURNAME);
        AdminWelcomeEmailData emailData = new AdminWelcomeEmailData(welcomeEmail);
        doNothing().when(rateLimitingService).validate(EMAIL, ADMIN_ACCOUNT_CREATION_EMAIL);

        EmailToSend result = emailService.handleEmailGeneration(emailData, ADMIN_ACCOUNT_CREATION_EMAIL);

        assertThat(result.getEmailAddress())
            .as(EMAIL_MESSAGE)
            .isEqualTo(EMAIL);

        assertThat(result.getTemplate())
            .as(TEMPLATE_MESSAGE)
            .isEqualTo(ADMIN_ACCOUNT_CREATION_EMAIL.getTemplate());
    }

    @Test
    void testHandleEmailGenerationOverRateLimit() {
        CreatedAdminWelcomeEmail welcomeEmail = new CreatedAdminWelcomeEmail(EMAIL, FORENAME, SURNAME);
        AdminWelcomeEmailData emailData = new AdminWelcomeEmailData(welcomeEmail);
        doThrow(TOO_MANY_EMAILS_EXCEPTION).when(rateLimitingService).validate(EMAIL, ADMIN_ACCOUNT_CREATION_EMAIL);

        assertThatThrownBy(() -> emailService.handleEmailGeneration(emailData, ADMIN_ACCOUNT_CREATION_EMAIL))
            .as("")
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);
    }

    @Test
    void testHandleBatchEmailGenerationWithinRateLimit() {
        LocationSubscriptionDeletion locationSubscriptionDeletion = new LocationSubscriptionDeletion(
            LOCATION_NAME, List.of(EMAIL1, EMAIL2, EMAIL3)
        );
        LocationSubscriptionDeletionEmailData emailData = new LocationSubscriptionDeletionEmailData(
            locationSubscriptionDeletion, REFERENCE_ID
        );

        when(rateLimitingService.isValid(anyString(), eq(DELETE_LOCATION_SUBSCRIPTION))).thenReturn(true);

        List<EmailToSend> results = emailService.handleBatchEmailGeneration(emailData, DELETE_LOCATION_SUBSCRIPTION);

        assertThat(results)
            .as(EMAIL_MESSAGE)
            .hasSize(3);

        assertThat(results.get(0).getEmailAddress())
            .as(EMAIL_MESSAGE)
            .isEqualTo(EMAIL1);

        assertThat(results.get(1).getEmailAddress())
            .as(EMAIL_MESSAGE)
            .isEqualTo(EMAIL2);

        assertThat(results.get(2).getEmailAddress())
            .as(EMAIL_MESSAGE)
            .isEqualTo(EMAIL3);

        assertThat(results.get(0).getTemplate())
            .as(TEMPLATE_MESSAGE)
            .isEqualTo(DELETE_LOCATION_SUBSCRIPTION.getTemplate());
    }

    @Test
    void testHandleBatchEmailGenerationWithSomeEmailsOverRateLimit() {
        LocationSubscriptionDeletion locationSubscriptionDeletion = new LocationSubscriptionDeletion(
            LOCATION_NAME, List.of(EMAIL1, EMAIL2, EMAIL3)
        );
        LocationSubscriptionDeletionEmailData emailData = new LocationSubscriptionDeletionEmailData(
            locationSubscriptionDeletion, REFERENCE_ID
        );

        when(rateLimitingService.isValid(EMAIL1, DELETE_LOCATION_SUBSCRIPTION)).thenReturn(false);
        when(rateLimitingService.isValid(EMAIL2, DELETE_LOCATION_SUBSCRIPTION)).thenReturn(true);
        when(rateLimitingService.isValid(EMAIL3, DELETE_LOCATION_SUBSCRIPTION)).thenReturn(false);

        List<EmailToSend> results = emailService.handleBatchEmailGeneration(emailData, DELETE_LOCATION_SUBSCRIPTION);

        assertThat(results)
            .as(EMAIL_MESSAGE)
            .hasSize(1);

        assertThat(results.get(0).getEmailAddress())
            .as(EMAIL_MESSAGE)
            .isEqualTo(EMAIL2);

        assertThat(results.get(0).getTemplate())
            .as(TEMPLATE_MESSAGE)
            .isEqualTo(DELETE_LOCATION_SUBSCRIPTION.getTemplate());
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
