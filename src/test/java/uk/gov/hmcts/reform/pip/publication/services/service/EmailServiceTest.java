package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class EmailServiceTest {

    private static final String EMAIL = "test@email.com";

    @Autowired
    private EmailService emailService;

    @MockBean
    private PersonalisationService personalisationService;

    @MockBean
    private EmailClient emailClient;

    private Map<String, Object> personalisation = new HashMap<>();

    private SendEmailResponse sendEmailResponse;

    @BeforeEach
    void setup() {
        sendEmailResponse = mock(SendEmailResponse.class);
        personalisation.put("Value", "OtherValue");
    }

    @Test
    void buildAadEmailReturnsSuccess() {
        CreatedAdminWelcomeEmail createdAdminWelcomeEmail = new CreatedAdminWelcomeEmail(EMAIL, "b", "c");

        when(personalisationService.buildAdminAccountPersonalisation(createdAdminWelcomeEmail))
            .thenReturn(personalisation);

        EmailToSend aadEmail = emailService.buildCreatedAdminWelcomeEmail(
            createdAdminWelcomeEmail, Templates.ADMIN_ACCOUNT_CREATION_EMAIL.template);

        assertEquals(EMAIL, aadEmail.getEmailAddress(), "Generated email does not match");
        assertEquals(personalisation, aadEmail.getPersonalisation(), "Personalisation does not match");
        assertNotNull(aadEmail.getReferenceId(), "Reference ID is present");
        assertEquals(Templates.ADMIN_ACCOUNT_CREATION_EMAIL.template, aadEmail.getTemplate(),
                     "Personalisation does not match");
    }

    @Test
    void existingUserWelcomeValidEmailReturnsSuccess() {
        WelcomeEmail createdWelcomeEmail = new WelcomeEmail(EMAIL, true);

        when(personalisationService.buildWelcomePersonalisation()).thenReturn(personalisation);

        EmailToSend aadEmail = emailService.buildWelcomeEmail(
            createdWelcomeEmail, Templates.EXISTING_USER_WELCOME_EMAIL.template);

        assertEquals(EMAIL, aadEmail.getEmailAddress(), "Generated email does not match");
        assertEquals(personalisation, aadEmail.getPersonalisation(), "Personalisation does not match");
        assertNotNull(aadEmail.getReferenceId(), "Reference ID is present");
        assertEquals(Templates.EXISTING_USER_WELCOME_EMAIL.template, aadEmail.getTemplate(),
                     "Personalisation does not match");
    }

    @Test
    void newUserWelcomeValidEmailReturnsSuccess() {
        WelcomeEmail createdWelcomeEmail = new WelcomeEmail(EMAIL, true);

        when(personalisationService.buildWelcomePersonalisation()).thenReturn(personalisation);

        EmailToSend aadEmail = emailService.buildWelcomeEmail(
            createdWelcomeEmail, Templates.NEW_USER_WELCOME_EMAIL.template);

        assertEquals(EMAIL, aadEmail.getEmailAddress(), "Generated email does not match");
        assertEquals(personalisation, aadEmail.getPersonalisation(), "Personalisation does not match");
        assertNotNull(aadEmail.getReferenceId(), "Reference ID is present");
        assertEquals(Templates.NEW_USER_WELCOME_EMAIL.template, aadEmail.getTemplate(),
                     "Personalisation does not match");
    }

    @Test
    void flatFileSubscriptionEmailReturnsSuccess() {
        UUID artefactId = UUID.randomUUID();

        Map<SubscriptionTypes, List<String>> subscriptions = new HashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of("1234"));

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(EMAIL);
        subscriptionEmail.setArtefactId(artefactId);
        subscriptionEmail.setSubscriptions(subscriptions);

        Artefact artefact = new Artefact();
        artefact.setArtefactId(artefactId);
        artefact.setIsFlatFile(true);

        when(personalisationService.buildFlatFileSubscriptionPersonalisation(subscriptionEmail, artefact))
            .thenReturn(personalisation);

        EmailToSend aadEmail = emailService.buildFlatFileSubscriptionEmail(
            subscriptionEmail, artefact, Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL.template);

        assertEquals(EMAIL, aadEmail.getEmailAddress(), "Generated email does not match");
        assertEquals(personalisation, aadEmail.getPersonalisation(), "Personalisation does not match");
        assertNotNull(aadEmail.getReferenceId(), "Reference ID is present");
        assertEquals(Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL.template, aadEmail.getTemplate(),
                     "Personalisation does not match");
    }

    @Test
    void rawDataSubscriptionEmailReturnsSuccess() {
        UUID artefactId = UUID.randomUUID();

        Map<SubscriptionTypes, List<String>> subscriptions = new HashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of("1234"));

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(EMAIL);
        subscriptionEmail.setArtefactId(artefactId);
        subscriptionEmail.setSubscriptions(subscriptions);

        Artefact artefact = new Artefact();
        artefact.setArtefactId(artefactId);
        artefact.setIsFlatFile(false);

        when(personalisationService.buildRawDataSubscriptionPersonalisation(subscriptionEmail, artefact))
            .thenReturn(personalisation);

        EmailToSend aadEmail = emailService.buildRawDataSubscriptionEmail(
            subscriptionEmail, artefact, Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.template);

        assertEquals(EMAIL, aadEmail.getEmailAddress(), "Generated email does not match");
        assertEquals(personalisation, aadEmail.getPersonalisation(), "Personalisation does not match");
        assertNotNull(aadEmail.getReferenceId(), "Reference ID is present");
        assertEquals(Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.template, aadEmail.getTemplate(),
                     "Personalisation does not match");
    }

    @Test
    void testSendEmailWithSuccess() throws NotificationClientException {
        EmailToSend emailToSend = new EmailToSend(EMAIL, Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.template,
                                                  personalisation, UUID.randomUUID().toString());

        when(emailClient.sendEmail(Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.template, EMAIL, personalisation,
                                   emailToSend.getReferenceId()))
            .thenReturn(sendEmailResponse);

        assertEquals(sendEmailResponse, emailService.sendEmail(emailToSend),
                     "Email response does not match expected response");
    }

    @Test
    void testSendEmailWithFailure() throws NotificationClientException {
        String exceptionMessage = "This is an exception";
        EmailToSend emailToSend = new EmailToSend(EMAIL, Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.template,
                                                  personalisation, UUID.randomUUID().toString());

        when(emailClient.sendEmail(Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.template, EMAIL, personalisation,
                                   emailToSend.getReferenceId()))
            .thenThrow(new NotificationClientException(exceptionMessage));

        NotifyException notificationClientException =
            assertThrows(NotifyException.class, () -> emailService.sendEmail(emailToSend),
                         "Exception has not been thrown");

        assertEquals(exceptionMessage, notificationClientException.getMessage(),
                     "Exception message does not match expected exception");
    }

}
