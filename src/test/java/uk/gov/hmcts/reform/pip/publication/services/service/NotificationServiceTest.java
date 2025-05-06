package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
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
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.LocationSubscriptionDeletionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.BulkSubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
import uk.gov.service.notify.SendEmailResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.ExcessiveImports")
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
    private static final String REQUESTER_EMAIL = "test_user@justice.gov.uk";
    private static final String EMAIL = "test@email.com";
    private static final WelcomeEmail VALID_BODY_NEW = new WelcomeEmail(
        EMAIL, false, FULL_NAME);
    private static final CreatedAdminWelcomeEmail VALID_BODY_AAD = new CreatedAdminWelcomeEmail(
        EMAIL, "test_forename", "test_surname");

    private static final Integer LOCATION_ID = 1;
    private static final String LOCATION_NAME = "Location Name";
    private static final UUID ARTEFACT_ID = UUID.randomUUID();
    private static final String SUCCESS_REF_ID = "successRefId";
    private static final byte[] TEST_BYTE = "Test byte".getBytes();
    private static final Map<String, Object> PERSONALISATION_MAP = Map.of("email", EMAIL);
    private static final String REFERENCE_ID_MESSAGE = "Reference ID does not match";

    private static final List<NoMatchArtefact> NO_MATCH_ARTEFACT_LIST = new ArrayList<>();
    private final EmailToSend validEmailBodyForEmailClient = new EmailToSend(VALID_BODY_NEW.getEmail(),
                                                                             Templates.BAD_BLOB_EMAIL.getTemplate(),
                                                                             personalisationMap,
                                                                             SUCCESS_REF_ID);

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

    @Mock
    private FileCreationService fileCreationService;

    @Mock
    private DataManagementService dataManagementService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setup() {
        NO_MATCH_ARTEFACT_LIST.add(new NoMatchArtefact(UUID.randomUUID(), "TEST", "1234"));
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of("1234"));
        systemAdminActionEmailBody = new DeleteLocationAction();
        systemAdminActionEmailBody.setRequesterEmail(REQUESTER_EMAIL);
        systemAdminActionEmailBody.setEmailList(List.of(EMAIL));
        systemAdminActionEmailBody.setChangeType(ChangeType.DELETE_LOCATION);
        systemAdminActionEmailBody.setActionResult(ActionResult.ATTEMPTED);

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

        ReflectionTestUtils.setField(notificationService, "piTeamEmail", "test@justice.gov.uk");
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
        when(emailService.sendEmail(validEmailBodyForEmailClient)).thenReturn(sendEmailResponse);
        when(sendEmailResponse.getReference()).thenReturn(Optional.of(SUCCESS_REF_ID));

        assertEquals(SUCCESS_REF_ID, notificationService.handleMediaApplicationReportingRequest(mediaApplicationList),
                     "Media applications report with valid payload should return successful referenceId.");

    }

    @Test
    void testValidPayloadReturnsSuccessUnidentifiedBlob() {
        when(emailService.handleEmailGeneration(any(UnidentifiedBlobEmailData.class), eq(Templates.BAD_BLOB_EMAIL)))
            .thenReturn(validEmailBodyForEmailClient);
        when(emailService.sendEmail(validEmailBodyForEmailClient)).thenReturn(sendEmailResponse);
        when(sendEmailResponse.getReference()).thenReturn(Optional.of(SUCCESS_REF_ID));

        assertEquals(SUCCESS_REF_ID, notificationService.unidentifiedBlobEmailRequest(NO_MATCH_ARTEFACT_LIST),
                     "Unidentified blob with valid payload should return successful referenceId.");
    }

    @Test
    void testHandleMiDataReportingReturnsSuccess() {
        when(emailService.handleEmailGeneration(any(MiDataReportingEmailData.class),
                                                eq(Templates.MI_DATA_REPORTING_EMAIL)))
            .thenReturn(validEmailBodyForEmailClient);
        when(emailService.sendEmail(validEmailBodyForEmailClient)).thenReturn(sendEmailResponse);
        when(sendEmailResponse.getReference()).thenReturn(Optional.of(SUCCESS_REF_ID));

        assertEquals(SUCCESS_REF_ID, notificationService.handleMiDataForReporting(),
                     "Handling MI data reporting notification should return successful reference ID");
    }

    @Test
    void testValidPayloadReturnsSuccessSystemAdminUpdateEmail() {
        when(emailService.handleBatchEmailGeneration(any(SystemAdminUpdateEmailData.class),
                                                     eq(Templates.SYSTEM_ADMIN_UPDATE_EMAIL)))
            .thenReturn(List.of(validEmailBodyForEmailClient));
        when(emailService.sendEmail(validEmailBodyForEmailClient)).thenReturn(sendEmailResponse);

        assertNotNull(notificationService.sendSystemAdminUpdateEmailRequest(systemAdminActionEmailBody),
                      REFERENCE_ID_MESSAGE);
    }

    @Test
    void testValidPayloadReturnsDeleteLocationSubscriptionEmail() {
        locationSubscriptionDeletionBody.setLocationId(String.valueOf(LOCATION_ID));
        locationSubscriptionDeletionBody.setSubscriberEmails(List.of(EMAIL));

        when(dataManagementService.getLocation(String.valueOf(LOCATION_ID))).thenReturn(location);
        when(emailService.handleBatchEmailGeneration(any(LocationSubscriptionDeletionEmailData.class),
                                                     eq(Templates.DELETE_LOCATION_SUBSCRIPTION)))
            .thenReturn(List.of(validEmailBodyForEmailClient));
        when(emailService.sendEmail(validEmailBodyForEmailClient)).thenReturn(sendEmailResponse);

        assertNotNull(notificationService.sendDeleteLocationSubscriptionEmail(locationSubscriptionDeletionBody),
                      REFERENCE_ID_MESSAGE);
    }
}
