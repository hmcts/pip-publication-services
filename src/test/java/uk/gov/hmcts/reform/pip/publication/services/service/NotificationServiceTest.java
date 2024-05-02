package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.ChangeType;
import uk.gov.hmcts.reform.pip.model.system.admin.DeleteLocationAction;
import uk.gov.hmcts.reform.pip.model.system.admin.SystemAdminAction;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.LocationSubscriptionDeletionEmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.MediaApplicationReportingEmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.MiDataReportingEmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.SystemAdminUpdateEmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.UnidentifiedBlobEmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
@SuppressWarnings("PMD.ExcessiveImports")
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
    private static final String EMAIL = "test@email.com";
    private static final WelcomeEmail VALID_BODY_NEW = new WelcomeEmail(
        EMAIL, false, FULL_NAME);
    private static final CreatedAdminWelcomeEmail VALID_BODY_AAD = new CreatedAdminWelcomeEmail(
        EMAIL, "test_forename", "test_surname");

    private static final Integer LOCATION_ID = 1;
    private static final String LOCATION_NAME = "Location Name";
    private static final String SUCCESS_REF_ID = "successRefId";
    private static final byte[] TEST_BYTE = "Test byte".getBytes();

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

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private FileCreationService fileCreationService;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    void setup() {
        NO_MATCH_ARTEFACT_LIST.add(new NoMatchArtefact(UUID.randomUUID(), "TEST", "1234"));
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
    void testValidPayloadReturnsSuccessMediaReport() {
        List<MediaApplication> mediaApplicationList = List.of(new MediaApplication(
            UUID.randomUUID(), "Test user", EMAIL, "Test employer",
            UUID.randomUUID().toString(), "test-image.png", LocalDateTime.now(),
            "REJECTED", LocalDateTime.now()));

        when(fileCreationService.createMediaApplicationReportingCsv(mediaApplicationList)).thenReturn(TEST_BYTE);
        when(emailService.handleEmailGeneration(any(MediaApplicationReportingEmailBody.class),
                                                eq(Templates.MEDIA_APPLICATION_REPORTING_EMAIL)))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, notificationService.handleMediaApplicationReportingRequest(mediaApplicationList),
                     "Media applications report with valid payload should return successful referenceId.");

    }

    @Test
    void testValidPayloadReturnsSuccessUnidentifiedBlob() {
        when(emailService.handleEmailGeneration(any(UnidentifiedBlobEmailBody.class), eq(Templates.BAD_BLOB_EMAIL)))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, notificationService.unidentifiedBlobEmailRequest(NO_MATCH_ARTEFACT_LIST),
                     "Unidentified blob with valid payload should return successful referenceId.");
    }

    @Test
    void testHandleMiDataReportingReturnsSuccess() {
        when(emailService.handleEmailGeneration(any(MiDataReportingEmailBody.class),
                                                eq(Templates.MI_DATA_REPORTING_EMAIL)))
            .thenReturn(validEmailBodyForEmailClient);

        assertEquals(SUCCESS_REF_ID, notificationService.handleMiDataForReporting(),
                     "Handling MI data reporting notification should return successful reference ID");
    }

    @Test
    void testValidPayloadReturnsSuccessSystemAdminUpdateEmail() {
        when(emailService.handleBatchEmailGeneration(any(SystemAdminUpdateEmailBody.class),
                                                     eq(Templates.SYSTEM_ADMIN_UPDATE_EMAIL)))
            .thenReturn(List.of(validEmailBodyForEmailClient));

        assertEquals(List.of(SUCCESS_REF_ID), notificationService
                         .sendSystemAdminUpdateEmailRequest(systemAdminActionEmailBody),
                     SUCCESS_REF_ID
        );
    }

    @Test
    void testValidPayloadReturnsDeleteLocationSubscriptionEmail() {
        locationSubscriptionDeletionBody.setLocationName(LOCATION_NAME);
        locationSubscriptionDeletionBody.setSubscriberEmails(List.of(EMAIL));
        when(emailService.handleBatchEmailGeneration(any(LocationSubscriptionDeletionEmailBody.class),
                                                     eq(Templates.DELETE_LOCATION_SUBSCRIPTION)))
            .thenReturn(List.of(validEmailBodyForEmailClient));

        assertEquals(List.of(SUCCESS_REF_ID), notificationService
            .sendDeleteLocationSubscriptionEmail(locationSubscriptionDeletionBody),
                     SUCCESS_REF_ID
        );
    }
}
