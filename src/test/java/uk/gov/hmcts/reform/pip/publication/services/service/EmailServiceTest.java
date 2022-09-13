package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientConfigurationTest;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"PMD.TooManyMethods"})
@SpringBootTest(classes = {Application.class, WebClientConfigurationTest.class})
@ActiveProfiles("test")
class EmailServiceTest {

    private static final String EMAIL = "test@email.com";
    private static final String FULL_NAME = "fullName";
    private static final String LAST_SIGNED_IN_DATE = "11 July 2022";

    @Autowired
    private EmailService emailService;

    @MockBean
    private PersonalisationService personalisationService;

    @MockBean
    private EmailClient emailClient;

    private final Map<String, Object> personalisation = new ConcurrentHashMap<>();

    private SendEmailResponse sendEmailResponse;

    private static final String GENERATED_EMAIL_MESSAGE = "Generated email does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";
    private static final String REFERENCE_ID_MESSAGE = "Reference ID is present";
    private static final String TEMPLATE_MESSAGE = "Template does not match";
    private static final byte[] TEST_BYTE = "Test byte".getBytes();
    private static final Map<String, String> LOCATIONS_MAP = new ConcurrentHashMap<>();

    @BeforeEach
    void setup() {
        LOCATIONS_MAP.put("test", "1234");
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

        assertEquals(EMAIL, aadEmail.getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, aadEmail.getPersonalisation(), PERSONALISATION_MESSAGE);
        assertNotNull(aadEmail.getReferenceId(), REFERENCE_ID_MESSAGE);
        assertEquals(Templates.ADMIN_ACCOUNT_CREATION_EMAIL.template, aadEmail.getTemplate(),
                     TEMPLATE_MESSAGE
        );
    }

    @Test
    void existingUserWelcomeValidEmailReturnsSuccess() {
        WelcomeEmail createdWelcomeEmail = new WelcomeEmail(EMAIL, true, FULL_NAME);

        when(personalisationService.buildWelcomePersonalisation(createdWelcomeEmail))
            .thenReturn(personalisation);

        EmailToSend aadEmail = emailService.buildWelcomeEmail(
            createdWelcomeEmail, Templates.EXISTING_USER_WELCOME_EMAIL.template);

        assertEquals(EMAIL, aadEmail.getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, aadEmail.getPersonalisation(), PERSONALISATION_MESSAGE);
        assertNotNull(aadEmail.getReferenceId(), REFERENCE_ID_MESSAGE);
        assertEquals(Templates.EXISTING_USER_WELCOME_EMAIL.template, aadEmail.getTemplate(),
                     TEMPLATE_MESSAGE);
    }

    @Test
    void flatFileSubscriptionEmailReturnsSuccess() {
        UUID artefactId = UUID.randomUUID();

        Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();
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

        assertEquals(EMAIL, aadEmail.getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, aadEmail.getPersonalisation(), PERSONALISATION_MESSAGE);
        assertNotNull(aadEmail.getReferenceId(), REFERENCE_ID_MESSAGE);
        assertEquals(Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL.template, aadEmail.getTemplate(),
                     TEMPLATE_MESSAGE);
    }

    @Test
    void rawDataSubscriptionEmailReturnsSuccess() {
        UUID artefactId = UUID.randomUUID();

        Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();
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

        assertEquals(EMAIL, aadEmail.getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, aadEmail.getPersonalisation(), PERSONALISATION_MESSAGE);
        assertNotNull(aadEmail.getReferenceId(), REFERENCE_ID_MESSAGE);
        assertEquals(Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.template, aadEmail.getTemplate(),
                     TEMPLATE_MESSAGE);
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

    @Test
    void duplicateMediaUserValidEmailReturnsSuccess() {
        DuplicatedMediaEmail duplicateMediaSetupEmail = new DuplicatedMediaEmail();
        duplicateMediaSetupEmail.setFullName("testname");
        duplicateMediaSetupEmail.setEmail(EMAIL);

        when(personalisationService.buildDuplicateMediaAccountPersonalisation(duplicateMediaSetupEmail))
            .thenReturn(personalisation);

        EmailToSend aadEmail = emailService.buildDuplicateMediaSetupEmail(
            duplicateMediaSetupEmail, Templates.MEDIA_DUPLICATE_ACCOUNT_EMAIL.template);

        assertEquals(EMAIL, aadEmail.getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, aadEmail.getPersonalisation(), PERSONALISATION_MESSAGE);
        assertNotNull(aadEmail.getReferenceId(), REFERENCE_ID_MESSAGE);
        assertEquals(Templates.MEDIA_DUPLICATE_ACCOUNT_EMAIL.template, aadEmail.getTemplate(),
                     TEMPLATE_MESSAGE);
    }

    @Test
    void testMediaApplicationReportingEmailReturnsSuccess() {

        when(personalisationService.buildMediaApplicationsReportingPersonalisation(TEST_BYTE))
            .thenReturn(personalisation);

        EmailToSend mediaReportingEmail = emailService
            .buildMediaApplicationReportingEmail(TEST_BYTE,
                                                 Templates.MEDIA_APPLICATION_REPORTING_EMAIL.template);
        assertEquals(EMAIL, mediaReportingEmail.getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, mediaReportingEmail.getPersonalisation(), PERSONALISATION_MESSAGE);
        assertEquals(Templates.MEDIA_APPLICATION_REPORTING_EMAIL.template, mediaReportingEmail.getTemplate(),
                     TEMPLATE_MESSAGE);
    }

    @Test
    void testUnidentifiedBlobEmailReturnsSuccess() {
        when(personalisationService.buildUnidentifiedBlobsPersonalisation(LOCATIONS_MAP))
            .thenReturn(personalisation);

        EmailToSend unidentifiedBlobEmail = emailService
            .buildUnidentifiedBlobsEmail(LOCATIONS_MAP,
                                         Templates.BAD_BLOB_EMAIL.template);

        assertEquals("joshua.blackmoor@justice.gov.uk", unidentifiedBlobEmail.getEmailAddress(),
                     GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, unidentifiedBlobEmail.getPersonalisation(),
                     PERSONALISATION_MESSAGE);
        assertEquals(Templates.BAD_BLOB_EMAIL.template, unidentifiedBlobEmail.getTemplate(),
                     TEMPLATE_MESSAGE);
    }

    @Test
    void testMediaVerificationEmailReturnsSuccess() {
        MediaVerificationEmail mediaVerificationEmailData = new MediaVerificationEmail(FULL_NAME, EMAIL);
        when(personalisationService.buildMediaVerificationPersonalisation(mediaVerificationEmailData))
            .thenReturn(personalisation);

        EmailToSend mediaVerificationEmail = emailService.buildMediaUserVerificationEmail(
            mediaVerificationEmailData, Templates.MEDIA_USER_VERIFICATION_EMAIL.template);

        assertEquals(EMAIL, mediaVerificationEmail.getEmailAddress(),
                     GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, mediaVerificationEmail.getPersonalisation(),
                     PERSONALISATION_MESSAGE);
        assertEquals(Templates.MEDIA_USER_VERIFICATION_EMAIL.template, mediaVerificationEmail.getTemplate(),
                     TEMPLATE_MESSAGE);
    }

    @Test
    void testInactiveUserNotificationEmailReturnsSuccess() {
        InactiveUserNotificationEmail inactiveUserNotificationEmail = new InactiveUserNotificationEmail(
            EMAIL, FULL_NAME, LAST_SIGNED_IN_DATE
        );
        when(personalisationService.buildInactiveUserNotificationPersonalisation(inactiveUserNotificationEmail))
            .thenReturn(personalisation);

        EmailToSend email = emailService.buildInactiveUserNotificationEmail(
            inactiveUserNotificationEmail, Templates.INACTIVE_USER_NOTIFICATION_EMAIL.template);

        assertThat(email)
            .extracting(
                EmailToSend::getEmailAddress,
                EmailToSend::getPersonalisation,
                EmailToSend::getTemplate
            )
            .containsExactly(
                EMAIL,
                personalisation,
                Templates.INACTIVE_USER_NOTIFICATION_EMAIL.template
            );

    }
}
