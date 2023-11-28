package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.ChangeType;
import uk.gov.hmcts.reform.pip.model.system.admin.DeleteLocationAction;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.configuration.RedisTestConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientTestConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.TooManyEmailsException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaRejectionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.ADMIN_ACCOUNT_CREATION_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.BAD_BLOB_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.DELETE_LOCATION_SUBSCRIPTION;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.EXISTING_USER_WELCOME_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.INACTIVE_USER_NOTIFICATION_EMAIL_AAD;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_APPLICATION_REPORTING_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_DUPLICATE_ACCOUNT_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_USER_REJECTION_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_USER_VERIFICATION_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MI_DATA_REPORTING_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.SYSTEM_ADMIN_UPDATE_EMAIL;

@SpringBootTest(classes = {Application.class, WebClientTestConfiguration.class, RedisTestConfiguration.class})
@DirtiesContext
@ActiveProfiles("test")
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports"})
class EmailServiceTest {

    private static final String EMAIL = "test@email.com";
    private static final String EMAIL2 = "test2@email.com";
    private static final String FULL_NAME = "fullName";
    private static final String LAST_SIGNED_IN_DATE = "11 July 2022";

    @Autowired
    private EmailService emailService;

    @MockBean
    private PersonalisationService personalisationService;

    @MockBean
    private EmailClient emailClient;

    @MockBean
    private RateLimitingService rateLimitingService;

    private final Map<String, Object> personalisation = new ConcurrentHashMap<>();

    private SendEmailResponse sendEmailResponse;

    private static final String GENERATED_EMAIL_SIZE_MESSAGE = "The number of emails does not match";
    private static final String GENERATED_EMAIL_MESSAGE = "Generated email does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";
    private static final String REFERENCE_ID_MESSAGE = "Reference ID is present";
    private static final String TEMPLATE_MESSAGE = "Template does not match";
    private static final String RATE_LIMIT_MESSAGE = "Rate limit exception does not match";

    private static final byte[] TEST_BYTE = "Test byte".getBytes();
    private static final List<NoMatchArtefact> NO_MATCH_ARTEFACT_LIST = new ArrayList<>();
    private static final String ERROR_MESSAGE = "Test message";
    private static final TooManyEmailsException TOO_MANY_EMAILS_EXCEPTION = new TooManyEmailsException(ERROR_MESSAGE);

    @BeforeEach
    void setup() {
        NO_MATCH_ARTEFACT_LIST.add(new NoMatchArtefact(UUID.randomUUID(), "TEST", "1234"));
        sendEmailResponse = mock(SendEmailResponse.class);
        personalisation.put("Value", "OtherValue");
    }

    @Test
    void adminAccountCreationEmailReturnsSuccess() {
        CreatedAdminWelcomeEmail createdAdminWelcomeEmail = new CreatedAdminWelcomeEmail(EMAIL, "b", "c");

        doNothing().when(rateLimitingService).validate(EMAIL, ADMIN_ACCOUNT_CREATION_EMAIL);
        when(personalisationService.buildAdminAccountPersonalisation(createdAdminWelcomeEmail))
            .thenReturn(personalisation);

        EmailToSend aadEmail = emailService.buildCreatedAdminWelcomeEmail(
            createdAdminWelcomeEmail, ADMIN_ACCOUNT_CREATION_EMAIL);

        assertEquals(EMAIL, aadEmail.getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, aadEmail.getPersonalisation(), PERSONALISATION_MESSAGE);
        assertNotNull(aadEmail.getReferenceId(), REFERENCE_ID_MESSAGE);
        assertEquals(ADMIN_ACCOUNT_CREATION_EMAIL.getTemplate(), aadEmail.getTemplate(),
                     TEMPLATE_MESSAGE
        );
    }

    @Test
    void adminAccountCreationEmailAboveRateLimit() {
        CreatedAdminWelcomeEmail createdAdminWelcomeEmail = new CreatedAdminWelcomeEmail(EMAIL, "b", "c");

        doThrow(TOO_MANY_EMAILS_EXCEPTION).when(rateLimitingService)
            .validate(EMAIL, ADMIN_ACCOUNT_CREATION_EMAIL);

        assertThatThrownBy(() -> emailService
            .buildCreatedAdminWelcomeEmail(createdAdminWelcomeEmail, ADMIN_ACCOUNT_CREATION_EMAIL))
            .as(RATE_LIMIT_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);

        verifyNoInteractions(personalisationService);
    }

    @Test
    void existingUserWelcomeValidEmailReturnsSuccess() {
        WelcomeEmail createdWelcomeEmail = new WelcomeEmail(EMAIL, true, FULL_NAME);

        doNothing().when(rateLimitingService).validate(EMAIL, EXISTING_USER_WELCOME_EMAIL);
        when(personalisationService.buildWelcomePersonalisation(createdWelcomeEmail))
            .thenReturn(personalisation);

        EmailToSend aadEmail = emailService.buildWelcomeEmail(
            createdWelcomeEmail, EXISTING_USER_WELCOME_EMAIL);

        assertEquals(EMAIL, aadEmail.getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, aadEmail.getPersonalisation(), PERSONALISATION_MESSAGE);
        assertNotNull(aadEmail.getReferenceId(), REFERENCE_ID_MESSAGE);
        assertEquals(EXISTING_USER_WELCOME_EMAIL.getTemplate(), aadEmail.getTemplate(),
                     TEMPLATE_MESSAGE
        );
    }

    @Test
    void existingUserWelcomeValidEmailAboveRateLimit() {
        WelcomeEmail createdWelcomeEmail = new WelcomeEmail(EMAIL, true, FULL_NAME);

        doThrow(TOO_MANY_EMAILS_EXCEPTION).when(rateLimitingService)
            .validate(EMAIL, EXISTING_USER_WELCOME_EMAIL);

        assertThatThrownBy(() -> emailService
            .buildWelcomeEmail(createdWelcomeEmail, EXISTING_USER_WELCOME_EMAIL))
            .as(RATE_LIMIT_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);

        verifyNoInteractions(personalisationService);
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

        doNothing().when(rateLimitingService)
            .validate(EMAIL, MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL);
        when(personalisationService.buildFlatFileSubscriptionPersonalisation(subscriptionEmail, artefact))
            .thenReturn(personalisation);

        EmailToSend aadEmail = emailService.buildFlatFileSubscriptionEmail(
            subscriptionEmail, artefact, MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL);

        assertEquals(EMAIL, aadEmail.getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, aadEmail.getPersonalisation(), PERSONALISATION_MESSAGE);
        assertNotNull(aadEmail.getReferenceId(), REFERENCE_ID_MESSAGE);
        assertEquals(MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL.getTemplate(), aadEmail.getTemplate(),
                     TEMPLATE_MESSAGE
        );
    }

    @Test
    void flatFileSubscriptionEmailAboveRateLimit() {
        Artefact artefact = new Artefact();
        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(EMAIL);

        doThrow(TOO_MANY_EMAILS_EXCEPTION).when(rateLimitingService)
            .validate(EMAIL, MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL);

        assertThatThrownBy(() -> emailService
            .buildFlatFileSubscriptionEmail(subscriptionEmail, artefact, MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL))
            .as(RATE_LIMIT_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);

        verifyNoInteractions(personalisationService);
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

        doNothing().when(rateLimitingService)
            .validate(EMAIL, MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL);
        when(personalisationService.buildRawDataSubscriptionPersonalisation(subscriptionEmail, artefact))
            .thenReturn(personalisation);

        EmailToSend aadEmail = emailService.buildRawDataSubscriptionEmail(
            subscriptionEmail, artefact, MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL);

        assertEquals(EMAIL, aadEmail.getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, aadEmail.getPersonalisation(), PERSONALISATION_MESSAGE);
        assertNotNull(aadEmail.getReferenceId(), REFERENCE_ID_MESSAGE);
        assertEquals(MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.getTemplate(), aadEmail.getTemplate(),
                     TEMPLATE_MESSAGE
        );
    }

    @Test
    void rawDataSubscriptionEmailAboveRateLimit() {
        Artefact artefact = new Artefact();
        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(EMAIL);

        doThrow(TOO_MANY_EMAILS_EXCEPTION).when(rateLimitingService)
            .validate(EMAIL, MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL);

        assertThatThrownBy(() -> emailService
            .buildRawDataSubscriptionEmail(subscriptionEmail, artefact, MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL))
            .as(RATE_LIMIT_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);

        verifyNoInteractions(personalisationService);
    }

    @Test
    void testSendEmailWithSuccess() throws NotificationClientException {
        EmailToSend emailToSend = new EmailToSend(EMAIL, MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.getTemplate(),
                                                  personalisation, UUID.randomUUID().toString()
        );

        when(emailClient.sendEmail(MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.getTemplate(), EMAIL, personalisation,
                                   emailToSend.getReferenceId()
        ))
            .thenReturn(sendEmailResponse);

        assertEquals(sendEmailResponse, emailService.sendEmail(emailToSend),
                     "Email response does not match expected response"
        );
    }

    @Test
    void testSendEmailWithFailure() throws NotificationClientException {
        String exceptionMessage = "This is an exception";
        EmailToSend emailToSend = new EmailToSend(EMAIL, MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.getTemplate(),
                                                  personalisation, UUID.randomUUID().toString()
        );

        when(emailClient.sendEmail(MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.getTemplate(), EMAIL, personalisation,
                                   emailToSend.getReferenceId()
        ))
            .thenThrow(new NotificationClientException(exceptionMessage));

        NotifyException notificationClientException =
            assertThrows(NotifyException.class, () -> emailService.sendEmail(emailToSend),
                         "Exception has not been thrown"
            );

        assertEquals(exceptionMessage, notificationClientException.getMessage(),
                     "Exception message does not match expected exception"
        );
    }

    @Test
    void duplicateMediaUserValidEmailReturnsSuccess() {
        DuplicatedMediaEmail duplicateMediaSetupEmail = new DuplicatedMediaEmail();
        duplicateMediaSetupEmail.setFullName("testname");
        duplicateMediaSetupEmail.setEmail(EMAIL);

        doNothing().when(rateLimitingService).validate(EMAIL, MEDIA_DUPLICATE_ACCOUNT_EMAIL);
        when(personalisationService.buildDuplicateMediaAccountPersonalisation(duplicateMediaSetupEmail))
            .thenReturn(personalisation);

        EmailToSend aadEmail = emailService.buildDuplicateMediaSetupEmail(
            duplicateMediaSetupEmail, MEDIA_DUPLICATE_ACCOUNT_EMAIL);

        assertEquals(EMAIL, aadEmail.getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, aadEmail.getPersonalisation(), PERSONALISATION_MESSAGE);
        assertNotNull(aadEmail.getReferenceId(), REFERENCE_ID_MESSAGE);
        assertEquals(MEDIA_DUPLICATE_ACCOUNT_EMAIL.getTemplate(), aadEmail.getTemplate(), TEMPLATE_MESSAGE);
    }

    @Test
    void duplicateMediaUserValidEmailAboveRateLimit() {
        DuplicatedMediaEmail duplicateMediaSetupEmail = new DuplicatedMediaEmail();
        duplicateMediaSetupEmail.setEmail(EMAIL);

        doThrow(TOO_MANY_EMAILS_EXCEPTION).when(rateLimitingService)
            .validate(EMAIL, MEDIA_DUPLICATE_ACCOUNT_EMAIL);

        assertThatThrownBy(() -> emailService
            .buildDuplicateMediaSetupEmail(duplicateMediaSetupEmail, MEDIA_DUPLICATE_ACCOUNT_EMAIL))
            .as(RATE_LIMIT_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);

        verifyNoInteractions(personalisationService);
    }

    @Test
    void testMediaApplicationReportingEmailReturnsSuccess() {
        doNothing().when(rateLimitingService)
            .validate(EMAIL, MEDIA_APPLICATION_REPORTING_EMAIL);
        when(personalisationService.buildMediaApplicationsReportingPersonalisation(TEST_BYTE))
            .thenReturn(personalisation);

        EmailToSend mediaReportingEmail = emailService
            .buildMediaApplicationReportingEmail(
                TEST_BYTE,
                MEDIA_APPLICATION_REPORTING_EMAIL
            );
        assertEquals(EMAIL, mediaReportingEmail.getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, mediaReportingEmail.getPersonalisation(), PERSONALISATION_MESSAGE);
        assertEquals(MEDIA_APPLICATION_REPORTING_EMAIL.getTemplate(), mediaReportingEmail.getTemplate(),
                     TEMPLATE_MESSAGE
        );
    }

    @Test
    void testMediaApplicationReportingEmailAboveRateLimit() {
        doThrow(TOO_MANY_EMAILS_EXCEPTION).when(rateLimitingService)
            .validate(EMAIL, MEDIA_APPLICATION_REPORTING_EMAIL);

        assertThatThrownBy(() -> emailService
            .buildMediaApplicationReportingEmail(TEST_BYTE, MEDIA_APPLICATION_REPORTING_EMAIL))
            .as(RATE_LIMIT_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);

        verifyNoInteractions(personalisationService);
    }

    @Test
    void testUnidentifiedBlobEmailReturnsSuccess() {
        doNothing().when(rateLimitingService).validate(EMAIL, BAD_BLOB_EMAIL);
        when(personalisationService.buildUnidentifiedBlobsPersonalisation(NO_MATCH_ARTEFACT_LIST))
            .thenReturn(personalisation);

        EmailToSend unidentifiedBlobEmail = emailService
            .buildUnidentifiedBlobsEmail(NO_MATCH_ARTEFACT_LIST, BAD_BLOB_EMAIL);

        assertEquals(EMAIL, unidentifiedBlobEmail.getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, unidentifiedBlobEmail.getPersonalisation(), PERSONALISATION_MESSAGE);
        assertEquals(BAD_BLOB_EMAIL.getTemplate(), unidentifiedBlobEmail.getTemplate(), TEMPLATE_MESSAGE);
    }

    @Test
    void testUnidentifiedBlobEmailAboveRateLimit() {
        doThrow(TOO_MANY_EMAILS_EXCEPTION).when(rateLimitingService)
            .validate(EMAIL, BAD_BLOB_EMAIL);

        assertThatThrownBy(() -> emailService
            .buildUnidentifiedBlobsEmail(NO_MATCH_ARTEFACT_LIST, BAD_BLOB_EMAIL))
            .as(RATE_LIMIT_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);

        verifyNoInteractions(personalisationService);
    }

    @Test
    void testMediaVerificationEmailReturnsSuccess() {
        MediaVerificationEmail mediaVerificationEmailData = new MediaVerificationEmail(FULL_NAME, EMAIL);

        doNothing().when(rateLimitingService).validate(EMAIL, MEDIA_USER_VERIFICATION_EMAIL);
        when(personalisationService.buildMediaVerificationPersonalisation(mediaVerificationEmailData))
            .thenReturn(personalisation);

        EmailToSend mediaVerificationEmail = emailService.buildMediaUserVerificationEmail(
            mediaVerificationEmailData, MEDIA_USER_VERIFICATION_EMAIL);

        assertEquals(EMAIL, mediaVerificationEmail.getEmailAddress(),
                     GENERATED_EMAIL_MESSAGE
        );
        assertEquals(personalisation, mediaVerificationEmail.getPersonalisation(),
                     PERSONALISATION_MESSAGE
        );
        assertEquals(MEDIA_USER_VERIFICATION_EMAIL.getTemplate(), mediaVerificationEmail.getTemplate(),
                     TEMPLATE_MESSAGE
        );
    }

    @Test
    void testMediaVerificationEmailAboveRateLimit() {
        MediaVerificationEmail mediaVerificationEmailData = new MediaVerificationEmail(FULL_NAME, EMAIL);
        doThrow(TOO_MANY_EMAILS_EXCEPTION).when(rateLimitingService)
            .validate(EMAIL, MEDIA_USER_VERIFICATION_EMAIL);

        assertThatThrownBy(() -> emailService
            .buildMediaUserVerificationEmail(mediaVerificationEmailData, MEDIA_USER_VERIFICATION_EMAIL))
            .as(RATE_LIMIT_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);

        verifyNoInteractions(personalisationService);
    }

    @Test
    void testBuildMediaApplicationRejectionEmail() throws IOException {
        Map<String, List<String>> reasons = new ConcurrentHashMap<>();

        reasons.put("notMedia", List.of(
            "The applicant is not an accredited member of the media.",
            "You can sign in with an existing MyHMCTS account. Or you can register your organisation at "
                + "https://www.gov.uk/guidance/myhmcts-online-case-management-for-legal-professionals"));

        reasons.put("noMatch", List.of(
            "Details provided do not match.",
            "The name, email address and Press ID do not match each other."));


        MediaRejectionEmail mediaRejectionEmail = new MediaRejectionEmail(
            "Test Name",
            "test_email@address.com",
            reasons
        );
        Map<String, Object> testPersonalisation = new ConcurrentHashMap<>();
        testPersonalisation.put("FULL_NAME", "Test Name");
        testPersonalisation.put("REJECT_REASONS", Arrays.asList("Reason 1", "Reason 2"));

        when(personalisationService.buildMediaRejectionPersonalisation(mediaRejectionEmail))
            .thenReturn(testPersonalisation);
        doNothing().when(rateLimitingService).validate(EMAIL, MEDIA_USER_REJECTION_EMAIL);

        EmailToSend emailToSend = emailService.buildMediaApplicationRejectionEmail(
            mediaRejectionEmail, MEDIA_USER_REJECTION_EMAIL
        );
        assertThat(emailToSend)
            .extracting(
                EmailToSend::getEmailAddress,
                EmailToSend::getPersonalisation,
                EmailToSend::getTemplate
            )
            .containsExactly(
                "test_email@address.com",
                testPersonalisation,
                MEDIA_USER_REJECTION_EMAIL.getTemplate()
            );
    }

    @Test
    void testBuildMediaApplicationRejectionEmailAboveRateLimit() {
        MediaRejectionEmail mediaRejectionEmail = new MediaRejectionEmail(FULL_NAME, EMAIL, Collections.emptyMap());
        doThrow(TOO_MANY_EMAILS_EXCEPTION).when(rateLimitingService)
            .validate(EMAIL, MEDIA_USER_REJECTION_EMAIL);

        assertThatThrownBy(() -> emailService
            .buildMediaApplicationRejectionEmail(mediaRejectionEmail, MEDIA_USER_REJECTION_EMAIL))
            .as(RATE_LIMIT_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);

        verifyNoInteractions(personalisationService);
    }

    @Test
    void testInactiveUserNotificationEmailReturnsSuccess() {
        InactiveUserNotificationEmail inactiveUserNotificationEmail = new InactiveUserNotificationEmail(
            EMAIL, FULL_NAME, "PI_AAD", LAST_SIGNED_IN_DATE
        );

        doNothing().when(rateLimitingService)
            .validate(EMAIL, INACTIVE_USER_NOTIFICATION_EMAIL_AAD);
        when(personalisationService.buildInactiveUserNotificationPersonalisation(inactiveUserNotificationEmail))
            .thenReturn(personalisation);

        EmailToSend email = emailService.buildInactiveUserNotificationEmail(
            inactiveUserNotificationEmail, INACTIVE_USER_NOTIFICATION_EMAIL_AAD);

        assertThat(email)
            .extracting(
                EmailToSend::getEmailAddress,
                EmailToSend::getPersonalisation,
                EmailToSend::getTemplate
            )
            .containsExactly(
                EMAIL,
                personalisation,
                INACTIVE_USER_NOTIFICATION_EMAIL_AAD.getTemplate()
            );
    }

    @Test
    void testInactiveUserNotificationEmailAboveRateLimit() {
        InactiveUserNotificationEmail inactiveUserNotificationEmail = new InactiveUserNotificationEmail(
            EMAIL, FULL_NAME, "PI_AAD", LAST_SIGNED_IN_DATE
        );
        doThrow(TOO_MANY_EMAILS_EXCEPTION).when(rateLimitingService)
            .validate(EMAIL, INACTIVE_USER_NOTIFICATION_EMAIL_AAD);

        assertThatThrownBy(() -> emailService
            .buildInactiveUserNotificationEmail(inactiveUserNotificationEmail,
                                                INACTIVE_USER_NOTIFICATION_EMAIL_AAD))
            .as(RATE_LIMIT_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);

        verifyNoInteractions(personalisationService);
    }

    @Test
    void testMiDataReportingEmailReturnsSuccess() {
        doNothing().when(rateLimitingService).validate(EMAIL, MI_DATA_REPORTING_EMAIL);
        when(personalisationService.buildMiDataReportingPersonalisation())
            .thenReturn(personalisation);

        EmailToSend email = emailService.buildMiDataReportingEmail(MI_DATA_REPORTING_EMAIL);

        assertThat(email)
            .as("Returned values do not match")
            .extracting(
                EmailToSend::getEmailAddress,
                EmailToSend::getPersonalisation,
                EmailToSend::getTemplate
            )
            .containsExactly(
                EMAIL,
                personalisation,
                MI_DATA_REPORTING_EMAIL.getTemplate()
            );
    }

    @Test
    void testMiDataReportingEmailAboveRateLimit() {
        doThrow(TOO_MANY_EMAILS_EXCEPTION).when(rateLimitingService)
            .validate(EMAIL, MI_DATA_REPORTING_EMAIL);

        assertThatThrownBy(() -> emailService
            .buildMiDataReportingEmail(MI_DATA_REPORTING_EMAIL))
            .as(RATE_LIMIT_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);

        verifyNoInteractions(personalisationService);
    }

    @Test
    void buildSystemAdminUpdateEmailEmailReturnsSuccess() {
        DeleteLocationAction systemAdminAction = new DeleteLocationAction();
        systemAdminAction.setRequesterName(FULL_NAME);
        systemAdminAction.setEmailList(List.of(EMAIL));
        systemAdminAction.setChangeType(ChangeType.DELETE_LOCATION);
        systemAdminAction.setActionResult(ActionResult.ATTEMPTED);

        when(rateLimitingService.isValid(EMAIL, SYSTEM_ADMIN_UPDATE_EMAIL))
            .thenReturn(true);
        when(personalisationService.buildSystemAdminUpdateEmailPersonalisation(systemAdminAction))
            .thenReturn(personalisation);

        List<EmailToSend> systemAdminEmail = emailService.buildSystemAdminUpdateEmail(
            systemAdminAction, SYSTEM_ADMIN_UPDATE_EMAIL);

        assertEquals(EMAIL, systemAdminEmail.get(0).getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, systemAdminEmail.get(0).getPersonalisation(), PERSONALISATION_MESSAGE);
        assertNotNull(systemAdminEmail.get(0).getReferenceId(), REFERENCE_ID_MESSAGE);
        assertEquals(SYSTEM_ADMIN_UPDATE_EMAIL.getTemplate(), systemAdminEmail.get(0).getTemplate(),
                     TEMPLATE_MESSAGE
        );
    }

    @Test
    void buildSystemAdminUpdateEmailEmailAboveRateLimit() {
        DeleteLocationAction systemAdminAction = new DeleteLocationAction();
        systemAdminAction.setRequesterName(FULL_NAME);
        systemAdminAction.setEmailList(List.of(EMAIL, EMAIL2));
        systemAdminAction.setChangeType(ChangeType.DELETE_LOCATION);
        systemAdminAction.setActionResult(ActionResult.ATTEMPTED);

        when(rateLimitingService.isValid(EMAIL, SYSTEM_ADMIN_UPDATE_EMAIL))
            .thenReturn(true);
        when(rateLimitingService.isValid(EMAIL2, SYSTEM_ADMIN_UPDATE_EMAIL))
            .thenReturn(false);
        when(personalisationService.buildSystemAdminUpdateEmailPersonalisation(systemAdminAction))
            .thenReturn(personalisation);

        List<EmailToSend> systemAdminEmail = emailService.buildSystemAdminUpdateEmail(
            systemAdminAction, SYSTEM_ADMIN_UPDATE_EMAIL);

        assertEquals(1, systemAdminEmail.size(), GENERATED_EMAIL_SIZE_MESSAGE);
        assertEquals(EMAIL, systemAdminEmail.get(0).getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, systemAdminEmail.get(0).getPersonalisation(), PERSONALISATION_MESSAGE);
        assertNotNull(systemAdminEmail.get(0).getReferenceId(), REFERENCE_ID_MESSAGE);
        assertEquals(SYSTEM_ADMIN_UPDATE_EMAIL.getTemplate(), systemAdminEmail.get(0).getTemplate(),
                     TEMPLATE_MESSAGE);
    }

    @Test
    void buildDeleteLocationSubscriptionEmailReturnsSuccess() {
        LocationSubscriptionDeletion locationSubscriptionDeletion = new LocationSubscriptionDeletion();
        locationSubscriptionDeletion.setLocationName("locationName");
        locationSubscriptionDeletion.setSubscriberEmails(List.of(EMAIL));

        when(rateLimitingService.isValid(EMAIL, DELETE_LOCATION_SUBSCRIPTION))
            .thenReturn(true);
        when(personalisationService.buildDeleteLocationSubscriptionEmailPersonalisation(locationSubscriptionDeletion))
            .thenReturn(personalisation);

        List<EmailToSend> emailToSends = emailService.buildDeleteLocationSubscriptionEmail(
            locationSubscriptionDeletion, DELETE_LOCATION_SUBSCRIPTION);

        assertEquals(EMAIL, emailToSends.get(0).getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, emailToSends.get(0).getPersonalisation(), PERSONALISATION_MESSAGE);
        assertNotNull(emailToSends.get(0).getReferenceId(), REFERENCE_ID_MESSAGE);
        assertEquals(DELETE_LOCATION_SUBSCRIPTION.getTemplate(), emailToSends.get(0).getTemplate(),
                     TEMPLATE_MESSAGE
        );
    }

    @Test
    void buildDeleteLocationSubscriptionEmailAboveRateLimit() {
        LocationSubscriptionDeletion locationSubscriptionDeletion = new LocationSubscriptionDeletion();
        locationSubscriptionDeletion.setSubscriberEmails(List.of(EMAIL, EMAIL2));

        when(rateLimitingService.isValid(EMAIL, DELETE_LOCATION_SUBSCRIPTION))
            .thenReturn(false);
        when(rateLimitingService.isValid(EMAIL2, DELETE_LOCATION_SUBSCRIPTION))
            .thenReturn(true);
        when(personalisationService.buildDeleteLocationSubscriptionEmailPersonalisation(locationSubscriptionDeletion))
            .thenReturn(personalisation);

        List<EmailToSend> emailToSends = emailService.buildDeleteLocationSubscriptionEmail(
            locationSubscriptionDeletion, DELETE_LOCATION_SUBSCRIPTION);

        assertEquals(1, emailToSends.size(), GENERATED_EMAIL_SIZE_MESSAGE);
        assertEquals(EMAIL2, emailToSends.get(0).getEmailAddress(), GENERATED_EMAIL_MESSAGE);
        assertEquals(personalisation, emailToSends.get(0).getPersonalisation(), PERSONALISATION_MESSAGE);
        assertNotNull(emailToSends.get(0).getReferenceId(), REFERENCE_ID_MESSAGE);
        assertEquals(DELETE_LOCATION_SUBSCRIPTION.getTemplate(), emailToSends.get(0).getTemplate(),
                     TEMPLATE_MESSAGE
        );
    }
}
