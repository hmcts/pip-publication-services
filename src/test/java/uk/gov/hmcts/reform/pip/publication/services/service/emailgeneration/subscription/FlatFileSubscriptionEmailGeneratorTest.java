package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription;

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
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.FlatFileSubscriptionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;
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
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
class FlatFileSubscriptionEmailGeneratorTest extends RedisConfigurationTestBase {
    private static final String EMAIL = "test@testing.com";
    private static final UUID ARTEFACT_ID = UUID.randomUUID();
    private static final Map<SubscriptionTypes, List<String>> SUBSCRIPTIONS = Map.of(
        SubscriptionTypes.LOCATION_ID, List.of("123")
    );
    private static final String LOCATION_NAME = "Location name";
    private static final ListType LIST_TYPE = ListType.SJP_PRESS_LIST;
    private static final byte[] FLAT_FILE = "Test byte".getBytes();

    private static final int FILE_RETENTION_WEEKS = 78;
    private static final RetentionPeriodDuration RETENTION_PERIOD_DURATION = new RetentionPeriodDuration(
        FILE_RETENTION_WEEKS, ChronoUnit.WEEKS
    );
    private static final String ERROR_MESSAGE = "Error message";

    private static final String LIST_TYPE_PERSONALISATION = "list_type";
    private static final String LINK_TO_FILE = "link_to_file";
    private static final String SUBSCRIPTION_PAGE_LINK = "subscription_page_link";
    private static final String CONTENT_DATE_PERSONALISATION = "content_date";

    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    private PersonalisationLinks personalisationLinks;
    private FlatFileSubscriptionEmailData emailData;

    @Autowired
    private NotifyConfigProperties notifyConfigProperties;

    @Autowired
    private FlatFileSubscriptionEmailGenerator emailGenerator;

    @BeforeEach
    void setup() {
        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(EMAIL);
        subscriptionEmail.setSubscriptions(SUBSCRIPTIONS);

        Artefact artefact = new Artefact();
        artefact.setArtefactId(ARTEFACT_ID);
        artefact.setListType(LIST_TYPE);
        artefact.setContentDate(LocalDateTime.of(2024, Month.MAY, 1, 0, 0));

        emailData = new FlatFileSubscriptionEmailData(subscriptionEmail, artefact, LOCATION_NAME, FLAT_FILE,
                                                      FILE_RETENTION_WEEKS);
        personalisationLinks = notifyConfigProperties.getLinks();
    }

    @Test
    void testFlatFileSubscriptionEmailSuccess() {
        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL.getTemplate());

        Map<String, Object> personalisation = result.getPersonalisation();

        softly.assertThat(personalisation.get(LIST_TYPE_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo("SJP Press List (Full list)");

        softly.assertThat(personalisation.get(LINK_TO_FILE))
            .as(PERSONALISATION_MESSAGE)
            .isNotNull();

        softly.assertThat(personalisation.get(SUBSCRIPTION_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getSubscriptionPageLink());

        softly.assertThat(personalisation.get(CONTENT_DATE_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo("01 May 2024");

        assertUploadFileContent(softly, (JSONObject) personalisation.get(LINK_TO_FILE));
        softly.assertAll();
    }

    @Test
    void testFlatFileSubscriptionEmailWithException() {
        try (MockedStatic<NotificationClient> mockStatic = mockStatic(NotificationClient.class)) {
            mockStatic.when(() -> NotificationClient.prepareUpload(eq(FLAT_FILE), eq(false),
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
