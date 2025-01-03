package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.reporting;

import org.assertj.core.api.SoftAssertions;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting.MediaApplicationReportingEmailData;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.RetentionPeriodDuration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_APPLICATION_REPORTING_EMAIL;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MediaApplicationReportingEmailGeneratorTest {
    private static final String EMAIL = "test@testing.com";
    private static final byte[] MEDIA_APPLICATION_CSV = "Test byte".getBytes();
    private static final int FILE_RETENTION_WEEKS = 78;
    private static final String ENV_NAME_ORIGINAL = "stg";
    private static final String ENV_NAME = "Staging";
    private static final String ERROR_MESSAGE = "Error message";

    private static final String LINK_TO_FILE = "link_to_file";
    private static final String ENV_NAME_PERSONALISATION = "env_name";

    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String REFERENCE_ID_MESSAGE = "Reference ID does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    private MediaApplicationReportingEmailData emailData;

    @Mock
    private PersonalisationLinks personalisationLinks;

    @InjectMocks
    private MediaApplicationReportingEmailGenerator emailGenerator;

    @BeforeEach
    void setup() {
        emailData = new MediaApplicationReportingEmailData(EMAIL, MEDIA_APPLICATION_CSV, FILE_RETENTION_WEEKS,
                                                           ENV_NAME_ORIGINAL);
    }

    @Test
    void testBuildMediaApplicationReportingEmailSuccess() {
        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(MEDIA_APPLICATION_REPORTING_EMAIL.getTemplate());

        softly.assertThat(result.getReferenceId())
            .as(REFERENCE_ID_MESSAGE)
            .isNotNull();

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
    void testBuildMediaApplicationReportingEmailWithException() {
        try (MockedStatic<NotificationClient> mockStatic = mockStatic(NotificationClient.class)) {
            mockStatic.when(() -> NotificationClient.prepareUpload(eq(MEDIA_APPLICATION_CSV), eq(false),
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
            .isEqualTo("78 weeks");
    }
}
