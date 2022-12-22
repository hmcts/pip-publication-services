package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.ChangeType;
import uk.gov.hmcts.reform.pip.model.system.admin.DeleteLocationAction;
import uk.gov.hmcts.reform.pip.model.system.admin.SystemAdminAction;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.models.request.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.publication.services.models.request.ThirdPartySubscriptionArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
import uk.gov.service.notify.SendEmailResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings({"PMD"})
class NotificationServiceTest {
    private final Map<String, Object> personalisationMap = Map.ofEntries(
        entry("email", VALID_BODY_AAD.getEmail()),
        entry("surname", VALID_BODY_AAD.getSurname()),
        entry("first_name", VALID_BODY_AAD.getForename()),
        entry("reset_password_link", "http://www.test.com"),
        entry("sign_in_page_link", "http://www.google.com"),
        entry("media_sign_in_link", "http://www.google.com")
    );

    private static final String FULL_NAME = "fullName";
    private static final String EMAIL = "test@email.com";
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

    private static final String TEST_EMAIL = "test@email.com";
    private static final String SUCCESS_REF_ID = "successRefId";
    private static final byte[] TEST_BYTE = "Test byte".getBytes();

    private static final Map<String, String> LOCATIONS_MAP = new ConcurrentHashMap<>();
    private final EmailToSend validEmailBodyForEmailClient = new EmailToSend(VALID_BODY_NEW.getEmail(),
                                                                             Templates.BAD_BLOB_EMAIL.template,
                                                                             personalisationMap,
                                                                             SUCCESS_REF_ID);
    private static final UUID RAND_UUID = UUID.randomUUID();
    private static final Integer LOCATION_ID = 1;
    private static final String LOCATION_NAME = "Location Name";


    private final EmailToSend validEmailBodyForDuplicateMediaUserClient = new EmailToSend(VALID_BODY_NEW.getEmail(),
        Templates.MEDIA_DUPLICATE_ACCOUNT_EMAIL.template,
        personalisationMap,
        SUCCESS_REF_ID
    );

    private SystemAdminAction systemAdminActionEmailBody;

    private final Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();

    private static final String EXISTING_REFERENCE_ID =
        "Existing user with valid JSON should return successful referenceId.";
    private final Artefact artefact = new Artefact();
    private final Location location = new Location();

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private FileCreationService fileCreationService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private DataManagementService dataManagementService;

    @BeforeEach
    void setup() {
        LOCATIONS_MAP.put("test", "1234");
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of("1234"));
        systemAdminActionEmailBody = new DeleteLocationAction();
        systemAdminActionEmailBody.setRequesterName(FULL_NAME);
        systemAdminActionEmailBody.setEmailList(List.of(EMAIL));
        systemAdminActionEmailBody.setChangeType(ChangeType.DELETE_LOCATION);
        systemAdminActionEmailBody.setActionResult(ActionResult.ATTEMPTED);

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
        assertEquals(SUCCESS_REF_ID, notificationService.handleWelcomeEmailRequest(VALID_BODY_EXISTING),
                     EXISTING_REFERENCE_ID
        );
    }

    @Test
    void testValidPayloadReturnsSuccessNew() {
        when(emailService.buildWelcomeEmail(VALID_BODY_NEW, Templates.MEDIA_NEW_ACCOUNT_SETUP.template))
            .thenReturn(validEmailBodyForEmailClient);
        assertEquals(SUCCESS_REF_ID, notificationService.handleWelcomeEmailRequest(VALID_BODY_NEW),
                     EXISTING_REFERENCE_ID
        );
    }

    @Test
    void testValidPayloadReturnsSuccessAzure() {
        when(emailService.buildCreatedAdminWelcomeEmail(VALID_BODY_AAD,
                                                        Templates.ADMIN_ACCOUNT_CREATION_EMAIL.template))
            .thenReturn(validEmailBodyForEmailClient);
        assertEquals(SUCCESS_REF_ID, notificationService.azureNewUserEmailRequest(VALID_BODY_AAD),
                     "Azure user with valid JSON should return successful referenceId.");
    }

    @Test
    void testValidPayloadReturnsSuccessMediaReport() {
        List<MediaApplication> mediaApplicationList = List.of(new MediaApplication(
            UUID.randomUUID(), "Test user", TEST_EMAIL, "Test employer",
            UUID.randomUUID().toString(), "test-image.png", LocalDateTime.now(),
            "REJECTED", LocalDateTime.now()));

        when(fileCreationService.createMediaApplicationReportingCsv(mediaApplicationList)).thenReturn(TEST_BYTE);

        when(emailService.buildMediaApplicationReportingEmail(TEST_BYTE,
                                                              Templates.MEDIA_APPLICATION_REPORTING_EMAIL.template))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, notificationService.handleMediaApplicationReportingRequest(mediaApplicationList),
                     "Media applications report with valid payload should return successful referenceId.");

    }

    @Test
    void testValidPayloadReturnsSuccessUnidentifiedBlob() {
        when(emailService.buildUnidentifiedBlobsEmail(LOCATIONS_MAP,
                                                      Templates.BAD_BLOB_EMAIL.template))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, notificationService.unidentifiedBlobEmailRequest(LOCATIONS_MAP),
                     "Unidentified blob with valid payload should return successful referenceId.");
    }

    @Test
    void testIsFlatFile() {
        artefact.setArtefactId(RAND_UUID);
        artefact.setIsFlatFile(true);

        when(dataManagementService.getArtefact(RAND_UUID)).thenReturn(artefact);
        when(dataManagementService.getLocation(LOCATION_ID.toString())).thenReturn(location);

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail("a@b.com");
        subscriptionEmail.setArtefactId(RAND_UUID);
        subscriptionEmail.setSubscriptions(subscriptions);

        when(emailService.buildFlatFileSubscriptionEmail(subscriptionEmail, artefact,
                                                        Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL.template))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, notificationService.subscriptionEmailRequest(subscriptionEmail),
                     "Subscription with flat file should return successful referenceId.");

    }

    @Test
    void testIsNotFlatFile() {
        artefact.setArtefactId(RAND_UUID);
        artefact.setIsFlatFile(false);

        when(dataManagementService.getArtefact(RAND_UUID)).thenReturn(artefact);
        when(dataManagementService.getLocation(LOCATION_ID.toString())).thenReturn(location);

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail("a@b.com");
        subscriptionEmail.setArtefactId(RAND_UUID);
        subscriptionEmail.setSubscriptions(subscriptions);

        when(emailService.buildRawDataSubscriptionEmail(subscriptionEmail, artefact,
                                                         Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.template))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, notificationService.subscriptionEmailRequest(subscriptionEmail),
                     "Subscription with raw data should return successful referenceId.");

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
        assertEquals(SUCCESS_REF_ID, notificationService.mediaDuplicateUserEmailRequest(createMediaSetupEmail),
                     EXISTING_REFERENCE_ID
        );
    }

    @Test
    void testValidPayloadReturnsSuccessMediaVerification() {
        when(emailService.buildMediaUserVerificationEmail(MEDIA_VERIFICATION_EMAIL,
                                                      Templates.MEDIA_USER_VERIFICATION_EMAIL.template))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, notificationService.mediaUserVerificationEmailRequest(MEDIA_VERIFICATION_EMAIL),
                     "Media user verification email successfully sent with referenceId: referenceId.");
    }

    @Test
    void testValidPayloadReturnsSuccessInactiveUserNotificationForAad() {
        when(emailService.buildInactiveUserNotificationEmail(INACTIVE_USER_NOTIFICATION_EMAIL_AAD,
                                                             Templates.INACTIVE_USER_NOTIFICATION_EMAIL_AAD.template))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, notificationService.inactiveUserNotificationEmailRequest(
            INACTIVE_USER_NOTIFICATION_EMAIL_AAD),
                     "Inactive user notification should return successful reference ID");
    }

    @Test
    void testValidPayloadReturnsSuccessInactiveUserNotificationForCft() {
        when(emailService.buildInactiveUserNotificationEmail(INACTIVE_USER_NOTIFICATION_EMAIL_CFT,
                                                             Templates.INACTIVE_USER_NOTIFICATION_EMAIL_CFT.template))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, notificationService.inactiveUserNotificationEmailRequest(
            INACTIVE_USER_NOTIFICATION_EMAIL_CFT),
                     "Inactive user notification should return successful reference ID");
    }

    @Test
    void testHandleMiDataReportingReturnsSuccess() {
        when(emailService.buildMiDataReportingEmail(Templates.MI_DATA_REPORTING_EMAIL.template))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, notificationService.handleMiDataForReporting(),
                     "Handling MI data reporting notification should return successful reference ID");
    }

    @Test
    void testValidPayloadReturnsSuccessSystemAdminUpdateEmail() {
        when(emailService.buildSystemAdminUpdateEmail(systemAdminActionEmailBody,
                                                           Templates.SYSTEM_ADMIN_UPDATE_EMAIL.template))
            .thenReturn(List.of(validEmailBodyForEmailClient));
        assertEquals(List.of(SUCCESS_REF_ID), notificationService
                         .sendSystemAdminUpdateEmailRequest(systemAdminActionEmailBody),
                     EXISTING_REFERENCE_ID
        );
    }
}
