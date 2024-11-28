package uk.gov.hmcts.reform.pip.publication.services.service;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.FileType;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.TooManyEmailsException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.FlatFileSubscriptionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.RawDataSubscriptionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.BulkSubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;
import uk.gov.service.notify.SendEmailResponse;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.TooManyMethods"})
class SubscriptionNotificationServiceTest extends RedisConfigurationTestBase {
    private static final String EMAIL = "test@email.com";
    private static final String FILE_CONTENT = "123";
    private static final String ARTEFACT_SUMMARY = "Test artefact summary";
    private static final UUID ARTEFACT_ID = UUID.randomUUID();
    private static final Integer LOCATION_ID = 1;
    private static final String LOCATION_NAME = "Location Name";
    private static final byte[] ARTEFACT_FLAT_FILE = "Test byte".getBytes();
    private static final String SUCCESS_REF_ID = "successRefId";
    private static final String TEST_EXCEPTION_MESSAGE = "Test Exception Message";
    private static final String REFERENCE_ID_MESSAGE = "Reference ID does not match";
    private static final Map<String, Object> PERSONALISATION_MAP = Map.of("email", EMAIL);

    private final Artefact artefact = new Artefact();

    EmailToSend validEmailBodyForEmailClientRawData;
    EmailToSend validEmailBodyForEmailClientFlatFile;

    private final Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();
    private final SubscriptionEmail subscriptionEmail = new SubscriptionEmail();

    private final BulkSubscriptionEmail bulkSubscriptionEmail = new BulkSubscriptionEmail();

    @Mock
    private SendEmailResponse sendEmailResponse;

    @MockBean
    private EmailService emailService;

    @MockBean
    private DataManagementService dataManagementService;

    @Autowired
    private SubscriptionNotificationService notificationService;

    @BeforeEach
    void setup() {
        validEmailBodyForEmailClientRawData = new EmailToSend(
            EMAIL, MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.getTemplate(), PERSONALISATION_MAP, SUCCESS_REF_ID
        );

        validEmailBodyForEmailClientFlatFile = new EmailToSend(
            EMAIL, MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL.getTemplate(), PERSONALISATION_MAP, SUCCESS_REF_ID
        );

        subscriptions.put(SubscriptionTypes.LOCATION_ID, List.of("1"));

        subscriptionEmail.setEmail(EMAIL);
        subscriptionEmail.setSubscriptions(subscriptions);

        bulkSubscriptionEmail.setArtefactId(ARTEFACT_ID);

        SubscriptionEmail individualSubscriptionEmail = new SubscriptionEmail();
        individualSubscriptionEmail.setEmail(EMAIL);
        individualSubscriptionEmail.setSubscriptions(subscriptions);

        bulkSubscriptionEmail.setSubscriptionEmails(List.of(individualSubscriptionEmail));

        artefact.setArtefactId(ARTEFACT_ID);
        artefact.setLocationId(LOCATION_ID.toString());

        when(sendEmailResponse.getReference()).thenReturn(Optional.of(SUCCESS_REF_ID));
        when(emailService.sendEmail(any())).thenReturn(sendEmailResponse);

        when(dataManagementService.getArtefactFile(ARTEFACT_ID, FileType.PDF, false)).thenReturn(FILE_CONTENT);
        when(dataManagementService.getArtefactFile(ARTEFACT_ID, FileType.PDF, true)).thenReturn(FILE_CONTENT);
        when(dataManagementService.getArtefactSummary(ARTEFACT_ID)).thenReturn(ARTEFACT_SUMMARY);
        when(dataManagementService.getArtefactFile(ARTEFACT_ID, FileType.EXCEL, false)).thenReturn(FILE_CONTENT);
        when(dataManagementService.getArtefactFlatFile(ARTEFACT_ID)).thenReturn(ARTEFACT_FLAT_FILE);
    }

    @Test
    void testBulkFlatFileSubscriptionEmailRequest() {
        artefact.setIsFlatFile(true);

        ArgumentCaptor<FlatFileSubscriptionEmailData> argument =
            ArgumentCaptor.forClass(FlatFileSubscriptionEmailData.class);

        when(emailService.handleEmailGeneration(argument.capture(),
                                                eq(MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL)))
            .thenReturn(validEmailBodyForEmailClientFlatFile);

        notificationService.flatFileBulkSubscriptionEmailRequest(bulkSubscriptionEmail, artefact, LOCATION_NAME,
                                                                 SUCCESS_REF_ID);

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

        assertEquals(SUCCESS_REF_ID, flatFileSubscriptionEmailData.getReferenceId(), REFERENCE_ID_MESSAGE);
    }

    @Test
    void testBulkRawDataSubscriptionEmailRequest() {
        artefact.setIsFlatFile(false);
        artefact.setListType(ListType.SJP_PUBLIC_LIST);
        artefact.setLanguage(Language.WELSH);

        ArgumentCaptor<RawDataSubscriptionEmailData> argument =
            ArgumentCaptor.forClass(RawDataSubscriptionEmailData.class);

        when(emailService.handleEmailGeneration(argument.capture(),
                                                eq(MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL)))
            .thenReturn(validEmailBodyForEmailClientRawData);

        notificationService.rawDataBulkSubscriptionEmailRequest(bulkSubscriptionEmail, artefact, LOCATION_NAME,
                                                                SUCCESS_REF_ID);

        RawDataSubscriptionEmailData rawDataSubscriptionEmailData = argument.getValue();

        assertEquals(artefact, rawDataSubscriptionEmailData.getArtefact(),
                     "Incorrect artefact set");
        assertEquals(EMAIL, rawDataSubscriptionEmailData.getEmail(),
                     "Incorrect email address set");
        assertEquals(LOCATION_NAME, rawDataSubscriptionEmailData.getLocationName(),
                     "Incorrect location name");
        assertEquals(ARTEFACT_SUMMARY, rawDataSubscriptionEmailData.getArtefactSummary(),
                          "Incorrect summary content");
        assertArrayEquals(Base64.getDecoder().decode(FILE_CONTENT), rawDataSubscriptionEmailData.getPdf(),
                          "Incorrect PDF content");
        assertArrayEquals(Base64.getDecoder().decode(FILE_CONTENT), rawDataSubscriptionEmailData.getExcel(),
                          "Incorrect excel content");

        assertEquals(SUCCESS_REF_ID, rawDataSubscriptionEmailData.getReferenceId(), REFERENCE_ID_MESSAGE);
    }

    @Test
    void testBulkSubscriptionRequestWhenMultipleSubscriptions() {
        artefact.setIsFlatFile(true);

        when(emailService.handleEmailGeneration(any(FlatFileSubscriptionEmailData.class),
                                                eq(MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL)))
            .thenReturn(validEmailBodyForEmailClientFlatFile);

        BulkSubscriptionEmail bulkSubscriptionEmailWithMultiple = new BulkSubscriptionEmail();
        bulkSubscriptionEmailWithMultiple.setArtefactId(ARTEFACT_ID);
        bulkSubscriptionEmailWithMultiple.setSubscriptionEmails(List.of(subscriptionEmail, subscriptionEmail));

        notificationService.flatFileBulkSubscriptionEmailRequest(bulkSubscriptionEmailWithMultiple, artefact,
                                                                 LOCATION_NAME, SUCCESS_REF_ID);

        verify(emailService, times(2))
            .handleEmailGeneration(any(FlatFileSubscriptionEmailData.class), eq(MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL));
    }

    @Test
    void testBulkSubscriptionRequestEmptySummaryWhenTooBig() {
        artefact.setIsFlatFile(false);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        artefact.setPayloadSize(1024F);

        ArgumentCaptor<RawDataSubscriptionEmailData> argument =
            ArgumentCaptor.forClass(RawDataSubscriptionEmailData.class);

        when(emailService.handleEmailGeneration(argument.capture(),
                                                eq(MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL)))
            .thenReturn(validEmailBodyForEmailClientRawData);

        notificationService.rawDataBulkSubscriptionEmailRequest(bulkSubscriptionEmail, artefact, LOCATION_NAME,
                                                                SUCCESS_REF_ID);

        RawDataSubscriptionEmailData rawDataSubscriptionEmailData = argument.getValue();

        assertEquals("", rawDataSubscriptionEmailData.getArtefactSummary(),
                     "Incorrect summary content");
    }

    @Test
    void testBulkSubscriptionRequestWhenAdditionalPdf() {
        artefact.setIsFlatFile(false);
        artefact.setListType(ListType.SJP_PRESS_REGISTER);
        artefact.setLanguage(Language.WELSH);

        ArgumentCaptor<RawDataSubscriptionEmailData> argument =
            ArgumentCaptor.forClass(RawDataSubscriptionEmailData.class);

        when(emailService.handleEmailGeneration(argument.capture(),
                                                eq(MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL)))
            .thenReturn(validEmailBodyForEmailClientRawData);

        notificationService.rawDataBulkSubscriptionEmailRequest(bulkSubscriptionEmail, artefact, LOCATION_NAME,
                                                                SUCCESS_REF_ID);

        RawDataSubscriptionEmailData rawDataSubscriptionEmailData = argument.getValue();

        assertArrayEquals(Base64.getDecoder().decode(FILE_CONTENT), rawDataSubscriptionEmailData.getAdditionalPdf(),
                          "Incorrect additional PDF content");
    }

    @Test
    void testBulkSubscriptionRequestNoAdditionalPdfWhenEnglish() {
        artefact.setIsFlatFile(false);
        artefact.setListType(ListType.SJP_PRESS_REGISTER);
        artefact.setLanguage(Language.ENGLISH);

        ArgumentCaptor<RawDataSubscriptionEmailData> argument =
            ArgumentCaptor.forClass(RawDataSubscriptionEmailData.class);

        when(emailService.handleEmailGeneration(argument.capture(),
                                                eq(MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL)))
            .thenReturn(validEmailBodyForEmailClientRawData);

        notificationService.rawDataBulkSubscriptionEmailRequest(bulkSubscriptionEmail, artefact, LOCATION_NAME,
                                                                SUCCESS_REF_ID);

        RawDataSubscriptionEmailData rawDataSubscriptionEmailData = argument.getValue();

        assertArrayEquals(new byte[0], rawDataSubscriptionEmailData.getAdditionalPdf(),
                          "Incorrect additional PDF content");
    }

    @Test
    void testBulkSubscriptionRequestNoExcel() {
        artefact.setIsFlatFile(false);
        artefact.setListType(ListType.SJP_PRESS_REGISTER);
        artefact.setLanguage(Language.ENGLISH);

        ArgumentCaptor<RawDataSubscriptionEmailData> argument =
            ArgumentCaptor.forClass(RawDataSubscriptionEmailData.class);

        when(emailService.handleEmailGeneration(argument.capture(),
                                                eq(MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL)))
            .thenReturn(validEmailBodyForEmailClientRawData);

        notificationService.rawDataBulkSubscriptionEmailRequest(bulkSubscriptionEmail, artefact, LOCATION_NAME,
                                                                SUCCESS_REF_ID);

        RawDataSubscriptionEmailData rawDataSubscriptionEmailData = argument.getValue();

        assertArrayEquals(new byte[0], rawDataSubscriptionEmailData.getExcel(),
                          "Incorrect excel content");
    }

    @Test
    void testBulkSubscriptionContinuesWhenNotifyException() {
        artefact.setIsFlatFile(true);

        when(emailService.handleEmailGeneration(any(FlatFileSubscriptionEmailData.class),
                                                eq(MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL)))
            .thenThrow(new NotifyException(TEST_EXCEPTION_MESSAGE));

        BulkSubscriptionEmail bulkSubscriptionEmailWithMultiple = new BulkSubscriptionEmail();
        bulkSubscriptionEmailWithMultiple.setArtefactId(ARTEFACT_ID);
        bulkSubscriptionEmailWithMultiple.setSubscriptionEmails(List.of(subscriptionEmail, subscriptionEmail));

        notificationService.flatFileBulkSubscriptionEmailRequest(bulkSubscriptionEmailWithMultiple, artefact,
                                                                 LOCATION_NAME, SUCCESS_REF_ID);

        verify(emailService, times(2))
            .handleEmailGeneration(any(FlatFileSubscriptionEmailData.class), eq(MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL));
    }

    @Test
    void testBulkSubscriptionContinuesWhenTooManyEmailsException() {
        artefact.setIsFlatFile(true);

        when(emailService.handleEmailGeneration(any(FlatFileSubscriptionEmailData.class),
                                                eq(MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL)))
            .thenThrow(new TooManyEmailsException(TEST_EXCEPTION_MESSAGE));

        BulkSubscriptionEmail bulkSubscriptionEmailWithMultiple = new BulkSubscriptionEmail();
        bulkSubscriptionEmailWithMultiple.setArtefactId(ARTEFACT_ID);
        bulkSubscriptionEmailWithMultiple.setSubscriptionEmails(List.of(subscriptionEmail, subscriptionEmail));

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionNotificationService.class)) {
            notificationService.flatFileBulkSubscriptionEmailRequest(bulkSubscriptionEmailWithMultiple, artefact,
                                                                     LOCATION_NAME, SUCCESS_REF_ID);

            verify(emailService, times(2)).handleEmailGeneration(
                    any(FlatFileSubscriptionEmailData.class),
                    eq(MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL)
            );

            assertEquals(2, logCaptor.getErrorLogs().size(),
                         "Incorrect number of error logs for too many emails");
            assertTrue(logCaptor.getErrorLogs().get(0).contains(TEST_EXCEPTION_MESSAGE),
                         "Incorrect number of error logs for too many emails");
        }
    }
}
