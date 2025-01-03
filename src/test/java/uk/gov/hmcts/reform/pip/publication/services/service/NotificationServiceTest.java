package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.FileType;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.ChangeType;
import uk.gov.hmcts.reform.pip.model.system.admin.DeleteLocationAction;
import uk.gov.hmcts.reform.pip.model.system.admin.SystemAdminAction;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting.MediaApplicationReportingEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting.MiDataReportingEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting.SystemAdminUpdateEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting.UnidentifiedBlobEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.FlatFileSubscriptionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.LocationSubscriptionDeletionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.RawDataSubscriptionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.BulkSubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;
import uk.gov.service.notify.SendEmailResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CouplingBetweenObjects"})
class NotificationServiceTest extends RedisConfigurationTestBase {
    private final Map<String, Object> personalisationMap = Map.ofEntries(
        entry("email", VALID_BODY_AAD.getEmail()),
        entry("surname", VALID_BODY_AAD.getSurname()),
        entry("first_name", VALID_BODY_AAD.getForename()),
        entry("reset_password_link", "http://www.test.com"),
        entry("sign_in_page_link", "http://www.google.com"),
        entry("media_sign_in_link", "http://www.google.com")
    );

    private static final String FULL_NAME = "fullName";
    private static final String REQUESTER_EMAIL = "test_user@justice.gov.uk";
    private static final String EMAIL = "test@email.com";
    private static final WelcomeEmail VALID_BODY_NEW = new WelcomeEmail(
        EMAIL, false, FULL_NAME);
    private static final CreatedAdminWelcomeEmail VALID_BODY_AAD = new CreatedAdminWelcomeEmail(
        EMAIL, "test_forename", "test_surname");

    private static final Integer LOCATION_ID = 1;
    private static final String LOCATION_NAME = "Location Name";
    private static final byte[] ARTEFACT_FLAT_FILE = "Test byte".getBytes();
    private static final UUID ARTEFACT_ID = UUID.randomUUID();
    private static final String ARTEFACT_SUMMARY = "Test artefact summary";
    private static final String SUCCESS_REF_ID = "successRefId";
    private static final byte[] TEST_BYTE = "Test byte".getBytes();
    private static final String FILE_CONTENT = "123";
    private static final Map<String, Object> PERSONALISATION_MAP = Map.of("email", EMAIL);
    private static final String REFERENCE_ID_MESSAGE = "Reference ID does not match";

    private static final List<NoMatchArtefact> NO_MATCH_ARTEFACT_LIST = new ArrayList<>();
    private final EmailToSend validEmailBodyForEmailClient = new EmailToSend(VALID_BODY_NEW.getEmail(),
                                                                             Templates.BAD_BLOB_EMAIL.getTemplate(),
                                                                             personalisationMap,
                                                                             SUCCESS_REF_ID);

    private final EmailToSend validEmailBodyForDuplicateMediaUserClient = new EmailToSend(VALID_BODY_NEW.getEmail(),
        Templates.MEDIA_DUPLICATE_ACCOUNT_EMAIL.getTemplate(),
        personalisationMap,
        SUCCESS_REF_ID
    );

    private SystemAdminAction systemAdminActionEmailBody;
    private LocationSubscriptionDeletion locationSubscriptionDeletionBody =
         new LocationSubscriptionDeletion();

    private final Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();
    private final Location location = new Location();
    private final Artefact artefact = new Artefact();
    EmailToSend validEmailBodyForEmailClientFlatFile;
    EmailToSend validEmailBodyForEmailClientRawData;

    private final SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
    private final BulkSubscriptionEmail bulkSubscriptionEmail = new BulkSubscriptionEmail();

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private DataManagementService dataManagementService;

    @MockBean
    private FileCreationService fileCreationService;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    void setup() {
        NO_MATCH_ARTEFACT_LIST.add(new NoMatchArtefact(UUID.randomUUID(), "TEST", "1234"));
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of("1234"));
        systemAdminActionEmailBody = new DeleteLocationAction();
        systemAdminActionEmailBody.setRequesterEmail(REQUESTER_EMAIL);
        systemAdminActionEmailBody.setEmailList(List.of(EMAIL));
        systemAdminActionEmailBody.setChangeType(ChangeType.DELETE_LOCATION);
        systemAdminActionEmailBody.setActionResult(ActionResult.ATTEMPTED);

        when(sendEmailResponse.getReference()).thenReturn(Optional.of(SUCCESS_REF_ID));
        when(emailService.sendEmail(validEmailBodyForEmailClient)).thenReturn(sendEmailResponse);
        when(emailService.sendEmail(validEmailBodyForDuplicateMediaUserClient)).thenReturn(sendEmailResponse);

        validEmailBodyForEmailClientRawData = new EmailToSend(
            EMAIL, MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.getTemplate(), PERSONALISATION_MAP, SUCCESS_REF_ID
        );

        validEmailBodyForEmailClientFlatFile = new EmailToSend(
            EMAIL, MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL.getTemplate(), PERSONALISATION_MAP, SUCCESS_REF_ID
        );

        subscriptionEmail.setEmail(EMAIL);
        subscriptionEmail.setSubscriptions(subscriptions);
        location.setLocationId(LOCATION_ID);
        location.setName(LOCATION_NAME);
        artefact.setArtefactId(ARTEFACT_ID);
        artefact.setLocationId(LOCATION_ID.toString());
        bulkSubscriptionEmail.setArtefactId(ARTEFACT_ID);
        bulkSubscriptionEmail.setSubscriptionEmails(List.of(subscriptionEmail));

        when(dataManagementService.getArtefact(ARTEFACT_ID)).thenReturn(artefact);
        when(dataManagementService.getLocation(String.valueOf(LOCATION_ID))).thenReturn(location);
        when(dataManagementService.getArtefactFlatFile(ARTEFACT_ID)).thenReturn(ARTEFACT_FLAT_FILE);
        when(dataManagementService.getArtefactSummary(ARTEFACT_ID)).thenReturn(ARTEFACT_SUMMARY);
        when(dataManagementService.getArtefactFile(ARTEFACT_ID, FileType.PDF, false))
            .thenReturn(FILE_CONTENT);
        when(dataManagementService.getArtefactFile(ARTEFACT_ID, FileType.EXCEL, false))
            .thenReturn(FILE_CONTENT);
    }

    @Test
    void testValidPayloadReturnsSuccessMediaReport() {
        List<MediaApplication> mediaApplicationList = List.of(new MediaApplication(
            UUID.randomUUID(), "Test user", EMAIL, "Test employer",
            UUID.randomUUID().toString(), "test-image.png", LocalDateTime.now(),
            "REJECTED", LocalDateTime.now()));

        when(fileCreationService.createMediaApplicationReportingCsv(mediaApplicationList)).thenReturn(TEST_BYTE);
        when(emailService.handleEmailGeneration(any(MediaApplicationReportingEmailData.class),
                                                eq(Templates.MEDIA_APPLICATION_REPORTING_EMAIL)))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, notificationService.handleMediaApplicationReportingRequest(mediaApplicationList),
                     "Media applications report with valid payload should return successful referenceId.");

    }

    @Test
    void testValidPayloadReturnsSuccessUnidentifiedBlob() {
        when(emailService.handleEmailGeneration(any(UnidentifiedBlobEmailData.class), eq(Templates.BAD_BLOB_EMAIL)))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, notificationService.unidentifiedBlobEmailRequest(NO_MATCH_ARTEFACT_LIST),
                     "Unidentified blob with valid payload should return successful referenceId.");
    }

    @Test
    void testHandleMiDataReportingReturnsSuccess() {
        when(emailService.handleEmailGeneration(any(MiDataReportingEmailData.class),
                                                eq(Templates.MI_DATA_REPORTING_EMAIL)))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, notificationService.handleMiDataForReporting(),
                     "Handling MI data reporting notification should return successful reference ID");
    }

    @Test
    void testValidPayloadReturnsSuccessSystemAdminUpdateEmail() {
        when(emailService.handleBatchEmailGeneration(any(SystemAdminUpdateEmailData.class),
                                                     eq(Templates.SYSTEM_ADMIN_UPDATE_EMAIL)))
            .thenReturn(List.of(validEmailBodyForEmailClient));

        assertNotNull(notificationService.sendSystemAdminUpdateEmailRequest(systemAdminActionEmailBody),
                      REFERENCE_ID_MESSAGE);
    }

    @Test
    void testValidPayloadReturnsDeleteLocationSubscriptionEmail() {
        locationSubscriptionDeletionBody.setLocationName(LOCATION_NAME);
        locationSubscriptionDeletionBody.setSubscriberEmails(List.of(EMAIL));
        when(emailService.handleBatchEmailGeneration(any(LocationSubscriptionDeletionEmailData.class),
                                                     eq(Templates.DELETE_LOCATION_SUBSCRIPTION)))
            .thenReturn(List.of(validEmailBodyForEmailClient));

        assertNotNull(notificationService.sendDeleteLocationSubscriptionEmail(locationSubscriptionDeletionBody),
                      REFERENCE_ID_MESSAGE);
    }

    @Test
    void testBulkSendSubscriptionEmailWithFlatFile() {
        artefact.setIsFlatFile(true);

        ArgumentCaptor<FlatFileSubscriptionEmailData> argument =
            ArgumentCaptor.forClass(FlatFileSubscriptionEmailData.class);

        when(emailService.handleEmailGeneration(argument.capture(),
                                                eq(MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL)))
            .thenReturn(validEmailBodyForEmailClientFlatFile);

        assertNotNull(notificationService.bulkSendSubscriptionEmail(bulkSubscriptionEmail), REFERENCE_ID_MESSAGE);

        FlatFileSubscriptionEmailData flatFileSubscriptionEmailData = argument.getValue();

        assertEquals(artefact, flatFileSubscriptionEmailData.getArtefact(),
                     "Incorrect artefact set");
        assertEquals(EMAIL, flatFileSubscriptionEmailData.getEmail(),
                     "Incorrect email address set");
        assertEquals(LOCATION_NAME, flatFileSubscriptionEmailData.getLocationName(),
                     "Incorrect location name");
        assertArrayEquals(
            flatFileSubscriptionEmailData.getArtefactFlatFile(),
            ARTEFACT_FLAT_FILE,
            "Incorrect artefact flat file"
        );
    }

    @Test
    void testBulkSendSubscriptionEmailRequestWithRawData() {
        artefact.setIsFlatFile(false);
        artefact.setListType(ListType.SJP_PUBLIC_LIST);
        artefact.setLanguage(Language.WELSH);

        ArgumentCaptor<RawDataSubscriptionEmailData> argument =
            ArgumentCaptor.forClass(RawDataSubscriptionEmailData.class);

        when(emailService.handleEmailGeneration(argument.capture(),
                                                eq(MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL)))
            .thenReturn(validEmailBodyForEmailClientRawData);

        assertNotNull(notificationService.bulkSendSubscriptionEmail(bulkSubscriptionEmail), REFERENCE_ID_MESSAGE);

        RawDataSubscriptionEmailData rawDataSubscriptionEmailData = argument.getValue();

        assertEquals(artefact, rawDataSubscriptionEmailData.getArtefact(),
                     "Incorrect artefact set");
        assertEquals(EMAIL, rawDataSubscriptionEmailData.getEmail(),
                     "Incorrect email address set");
        assertEquals(LOCATION_NAME, rawDataSubscriptionEmailData.getLocationName(),
                     "Incorrect location name");
        assertEquals(ARTEFACT_SUMMARY, rawDataSubscriptionEmailData.getArtefactSummary(),
                     "Incorrect PDF content");
        assertArrayEquals(Base64.getDecoder().decode(FILE_CONTENT), rawDataSubscriptionEmailData.getPdf(),
                          "Incorrect PDF content");
        assertArrayEquals(Base64.getDecoder().decode(FILE_CONTENT), rawDataSubscriptionEmailData.getExcel(),
                          "Incorrect excel content");
    }
}
