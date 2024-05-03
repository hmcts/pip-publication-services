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
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientTestConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.MiDataReportingEmailData;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.RetentionPeriodDuration;

import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MI_DATA_REPORTING_EMAIL;

@SpringBootTest(classes = {Application.class, WebClientTestConfiguration.class})
@DirtiesContext
@ActiveProfiles("test")
class MiDataReportingEmailGeneratorTest {
    private static final String EMAIL = "test@testing.com";
    private static final byte[] EXCEL = "Test byte".getBytes();
    private static final int FILE_RETENTION_WEEKS = 78;
    private static final RetentionPeriodDuration RETENTION_PERIOD_DURATION = new RetentionPeriodDuration(
        FILE_RETENTION_WEEKS, ChronoUnit.WEEKS
    );
    private static final String ENV_NAME_ORIGINAL = "stg";
    private static final String ENV_NAME = "Staging";
    private static final String ERROR_MESSAGE = "Error message";

    private static final String LINK_TO_FILE = "link_to_file";
    private static final String ENV_NAME_PERSONALISATION = "env_name";

    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    private PersonalisationLinks personalisationLinks;
    private MiDataReportingEmailData emailData;

    @Autowired
    private NotifyConfigProperties notifyConfigProperties;

    @Autowired
    private MiDataReportingEmailGenerator emailGenerator;

    @BeforeEach
    void setup() {
        emailData = new MiDataReportingEmailData(EMAIL, EXCEL, FILE_RETENTION_WEEKS, ENV_NAME_ORIGINAL);
        personalisationLinks = notifyConfigProperties.getLinks();
    }

    @Test
    void testBuildMiDataReportingEmailSuccess() {
        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(MI_DATA_REPORTING_EMAIL.getTemplate());

        Map<String, Object> personalisation = result.getPersonalisation();

        softly.assertThat(personalisation.get(LINK_TO_FILE))
            .as(PERSONALISATION_MESSAGE)
            .isNotNull();

        softly.assertThat(personalisation.get(ENV_NAME_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(ENV_NAME);

        assertUploadFileContent(softly, (JSONObject) personalisation.get(LINK_TO_FILE));
        softly.assertAll();
    }

    @Test
    void testBuildMiDataReportingEmailWithException() {
        try (MockedStatic<NotificationClient> mockStatic = mockStatic(NotificationClient.class)) {
            mockStatic.when(() -> NotificationClient.prepareUpload(eq(EXCEL), eq(false),
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
