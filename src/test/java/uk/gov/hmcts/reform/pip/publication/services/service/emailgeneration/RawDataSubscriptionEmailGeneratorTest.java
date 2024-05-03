package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import org.assertj.core.api.SoftAssertions;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.RawDataSubscriptionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.RetentionPeriodDuration;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
class RawDataSubscriptionEmailGeneratorTest {
    private static final String EMAIL = "test@testing.com";
    private static final UUID ARTEFACT_ID = UUID.randomUUID();
    private static final Map<SubscriptionTypes, List<String>> SUBSCRIPTIONS = Map.of(
        SubscriptionTypes.LOCATION_ID, List.of("123")
    );
    private static final String LOCATION_NAME = "Location name";
    private static final String ARTEFACT_SUMMARY = "Summary";
    private static final byte[] FILE_DATA = "Test byte".getBytes();

    private static final int FILE_RETENTION_WEEKS = 78;
    private static final RetentionPeriodDuration RETENTION_PERIOD_DURATION = new RetentionPeriodDuration(
        FILE_RETENTION_WEEKS, ChronoUnit.WEEKS
    );
    private static final String ERROR_MESSAGE = "Error message";

    private static final String CASE_NUMBER_PERSONALISATION = "case_num";
    private static final String CASE_URN_PERSONALISATION = "case_urn";
    private static final String LOCATION_PERSONALISATION = "locations";
    private static final String LIST_TYPE_PERSONALISATION = "list_type";
    private static final String START_PAGE_LINK = "start_page_link";
    private static final String SUBSCRIPTION_PAGE_LINK = "subscription_page_link";
    private static final String SUMMARY_PERSONALISATION =  "testing_of_array";
    private static final String CONTENT_DATE_PERSONALISATION = "content_date";
    private static final String PDF_LINK_TO_FILE = "pdf_link_to_file";
    private static final String ENGLISH_PDF_LINK_TO_FILE = "english_pdf_link_to_file";
    private static final String WELSH_PDF_LINK_TO_FILE = "welsh_pdf_link_to_file";
    private static final String EXCEL_LINK_TO_FILE = "excel_link_to_file";

    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    private PersonalisationLinks personalisationLinks;
    private RawDataSubscriptionEmailData emailData;
    private final SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
    private final Artefact artefact = new Artefact();

    @Autowired
    private NotifyConfigProperties notifyConfigProperties;

    @Autowired
    private RawDataSubscriptionEmailGenerator emailGenerator;

    @BeforeEach
    void setup() {
        subscriptionEmail.setEmail(EMAIL);
        subscriptionEmail.setSubscriptions(SUBSCRIPTIONS);
        subscriptionEmail.setArtefactId(ARTEFACT_ID);

        artefact.setArtefactId(ARTEFACT_ID);
        artefact.setContentDate(LocalDateTime.of(2024, Month.APRIL, 30, 0, 0));

        personalisationLinks = notifyConfigProperties.getLinks();
    }

    @Test
    void testRawDataSubscriptionEmailSuccess() {
        artefact.setLanguage(Language.ENGLISH);
        artefact.setListType(ListType.SJP_PUBLIC_LIST);
        emailData = new RawDataSubscriptionEmailData(subscriptionEmail, artefact, ARTEFACT_SUMMARY, FILE_DATA,
                                                     new byte[0], FILE_DATA, LOCATION_NAME, FILE_RETENTION_WEEKS);

        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.getTemplate());

        Map<String, Object> personalisation = result.getPersonalisation();

        softly.assertThat(personalisation.get(CASE_NUMBER_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo("");

        softly.assertThat(personalisation.get(CASE_URN_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo("");

        softly.assertThat(personalisation.get(LOCATION_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(LOCATION_NAME);

        softly.assertThat(personalisation.get(LIST_TYPE_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo("SJP Public List");

        softly.assertThat(personalisation.get(START_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getStartPageLink());

        softly.assertThat(personalisation.get(SUBSCRIPTION_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getSubscriptionPageLink());

        softly.assertThat(personalisation.get(SUMMARY_PERSONALISATION))
                .as(PERSONALISATION_MESSAGE)
            .isEqualTo(ARTEFACT_SUMMARY);

        softly.assertThat(personalisation.get(CONTENT_DATE_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo("30 April 2024");

        softly.assertThat(personalisation.get(ENGLISH_PDF_LINK_TO_FILE))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo("");

        softly.assertThat(personalisation.get(WELSH_PDF_LINK_TO_FILE))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo("");

        assertUploadFileContent(softly, (JSONObject) personalisation.get(PDF_LINK_TO_FILE));
        assertUploadFileContent(softly, (JSONObject) personalisation.get(EXCEL_LINK_TO_FILE));

        softly.assertAll();
    }

    @Test
    void testRawDataSubscriptionEmailWithWelshArtefactSuccess() {
        artefact.setLanguage(Language.WELSH);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        emailData = new RawDataSubscriptionEmailData(subscriptionEmail, artefact, ARTEFACT_SUMMARY, FILE_DATA,
                                                     FILE_DATA, new byte[0], LOCATION_NAME, FILE_RETENTION_WEEKS);

        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.getTemplate());

        Map<String, Object> personalisation = result.getPersonalisation();

        softly.assertThat(personalisation.get(CASE_NUMBER_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo("");

        softly.assertThat(personalisation.get(CASE_URN_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo("");

        softly.assertThat(personalisation.get(LOCATION_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(LOCATION_NAME);

        softly.assertThat(personalisation.get(LIST_TYPE_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo("Civil Daily Cause List");

        softly.assertThat(personalisation.get(START_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getStartPageLink());

        softly.assertThat(personalisation.get(SUBSCRIPTION_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getSubscriptionPageLink());

        softly.assertThat(personalisation.get(SUMMARY_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(ARTEFACT_SUMMARY);

        softly.assertThat(personalisation.get(CONTENT_DATE_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo("30 April 2024");

        softly.assertThat(personalisation.get(PDF_LINK_TO_FILE))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo("");

        softly.assertThat(personalisation.get(EXCEL_LINK_TO_FILE))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo("");

        assertUploadFileContent(softly, (JSONObject) personalisation.get(ENGLISH_PDF_LINK_TO_FILE));
        assertUploadFileContent(softly, (JSONObject) personalisation.get(WELSH_PDF_LINK_TO_FILE));

        softly.assertAll();
    }

    @Test
    void testRawDataSubscriptionEmailWithException() {
        artefact.setLanguage(Language.ENGLISH);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);

        emailData = new RawDataSubscriptionEmailData(subscriptionEmail, artefact, ARTEFACT_SUMMARY, FILE_DATA,
                                                     FILE_DATA, new byte[0], LOCATION_NAME, FILE_RETENTION_WEEKS);

        try (MockedStatic<NotificationClient> mockStatic = mockStatic(NotificationClient.class)) {
            mockStatic.when(() -> NotificationClient.prepareUpload(eq(FILE_DATA), eq(false),
                                                                   any(RetentionPeriodDuration.class)))
                .thenThrow(new NotificationClientException(ERROR_MESSAGE));
            assertThatThrownBy(() -> emailGenerator.buildEmail(emailData, personalisationLinks))
                .isInstanceOf(NotifyException.class)
                .hasMessage(ERROR_MESSAGE);
        }
    }

    private void assertUploadFileContent(SoftAssertions softly, JSONObject fileToBeUploaded) {
        softly.assertThat(fileToBeUploaded.get("file"))
            .as(PERSONALISATION_MESSAGE)
            .isNotNull();

        softly.assertThat(fileToBeUploaded.get("filename"))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(JSONObject.NULL);

        softly.assertThat(fileToBeUploaded.get("confirm_email_before_download"))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(false);

        softly.assertThat(fileToBeUploaded.get("retention_period"))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(RETENTION_PERIOD_DURATION.toString());
    }
}
