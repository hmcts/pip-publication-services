package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.models.request.ThirdPartySubscription;
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
class NotificationServiceTest {
    private final Map<String, Object> personalisationMap = Map.ofEntries(
        entry("email", VALID_BODY_AAD.getEmail()),
        entry("surname", VALID_BODY_AAD.getSurname()),
        entry("first_name", VALID_BODY_AAD.getForename()),
        entry("reset_password_link", "http://www.test.com"),
        entry("sign_in_page_link", "http://www.google.com")
    );

    private static final String TEST_EMAIL = "test@email.com";

    private static final WelcomeEmail VALID_BODY_EXISTING = new WelcomeEmail(
        TEST_EMAIL, true);
    private static final WelcomeEmail VALID_BODY_NEW = new WelcomeEmail(
        TEST_EMAIL, false);
    private static final CreatedAdminWelcomeEmail VALID_BODY_AAD = new CreatedAdminWelcomeEmail(
        TEST_EMAIL, "test_forename", "test_surname");
    static final String SUCCESS_REF_ID = "successRefId";
    private static final byte[] TEST_BYTE = "Test byte".getBytes();

    private static final String SUCCESS_API_SENT = "Successfully sent list to testUrl";
    private static final Map<String, String> LOCATIONS_MAP = new ConcurrentHashMap<>();
    private final EmailToSend validEmailBodyForEmailClient = new EmailToSend(VALID_BODY_NEW.getEmail(),
                                                                             Templates.NEW_USER_WELCOME_EMAIL.template,
                                                                             personalisationMap,
                                                                             SUCCESS_REF_ID);
    private static final UUID RAND_UUID = UUID.randomUUID();
    private static final String API_DESTINATION = "testUrl";

    private final Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();

    private final Artefact artefact = new Artefact();

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private CsvCreationService csvCreationService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private DataManagementService dataManagementService;

    @MockBean
    private ThirdPartyService thirdPartyService;

    @BeforeEach
    void setup() {
        LOCATIONS_MAP.put("test", "1234");
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of("1234"));
        when(sendEmailResponse.getReference()).thenReturn(Optional.of(SUCCESS_REF_ID));
        when(emailService.sendEmail(validEmailBodyForEmailClient)).thenReturn(sendEmailResponse);
    }

    @Test
    void testValidPayloadReturnsSuccessExisting() {
        when(emailService.buildWelcomeEmail(VALID_BODY_EXISTING, Templates.EXISTING_USER_WELCOME_EMAIL.template))
            .thenReturn(validEmailBodyForEmailClient);
        assertEquals(SUCCESS_REF_ID, notificationService.handleWelcomeEmailRequest(VALID_BODY_EXISTING),
                     "Existing user with valid JSON should return successful referenceId."
        );
    }

    @Test
    void testValidPayloadReturnsSuccessNew() {
        when(emailService.buildWelcomeEmail(VALID_BODY_NEW, Templates.NEW_USER_WELCOME_EMAIL.template))
            .thenReturn(validEmailBodyForEmailClient);
        assertEquals(SUCCESS_REF_ID, notificationService.handleWelcomeEmailRequest(VALID_BODY_NEW),
                     "Existing user with valid JSON should return successful referenceId."
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

        when(csvCreationService.createMediaApplicationReportingCsv(mediaApplicationList)).thenReturn(TEST_BYTE);

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
    void testHandleThirdPartyFlatFile() {
        artefact.setArtefactId(RAND_UUID);
        artefact.setIsFlatFile(true);
        byte[] file = new byte[10];
        when(dataManagementService.getArtefact(RAND_UUID)).thenReturn(artefact);
        when(dataManagementService.getArtefactFlatFile(RAND_UUID)).thenReturn(file);
        when(thirdPartyService.handleThirdPartyCall(API_DESTINATION, file)).thenReturn(SUCCESS_REF_ID);

        ThirdPartySubscription subscription = new ThirdPartySubscription();
        subscription.setArtefactId(RAND_UUID);
        subscription.setApiDestination(API_DESTINATION);

        assertEquals(SUCCESS_API_SENT, notificationService.handleThirdParty(subscription),
                     "Api subscription with flat file should return successful referenceId.");
    }

    @Test
    void testHandleThirdPartyJson() {
        artefact.setArtefactId(RAND_UUID);
        artefact.setIsFlatFile(false);
        String jsonPayload = "test";
        when(dataManagementService.getArtefact(RAND_UUID)).thenReturn(artefact);
        when(dataManagementService.getArtefactJsonBlob(RAND_UUID)).thenReturn(jsonPayload);
        when(thirdPartyService.handleThirdPartyCall(API_DESTINATION, jsonPayload)).thenReturn(SUCCESS_REF_ID);

        ThirdPartySubscription subscription = new ThirdPartySubscription();
        subscription.setArtefactId(RAND_UUID);
        subscription.setApiDestination(API_DESTINATION);

        assertEquals(SUCCESS_API_SENT, notificationService.handleThirdParty(subscription),
                     "Api subscription with flat file should return successful referenceId.");
    }
}
