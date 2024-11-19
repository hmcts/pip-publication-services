package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.MediaAccountRejectionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaRejectionEmail;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_USER_REJECTION_EMAIL;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
class MediaAccountRejectionEmailGeneratorTest extends RedisConfigurationTestBase {
    private static final String EMAIL = "test@testing.com";
    private static final String FULL_NAME = "Full name";
    private static final Map<String, List<String>> REASONS = Map.of("1", List.of("reason", "description"));

    private static final String FULL_NAME_PERSONALISATION = "full-name";
    private static final String REJECTION_REASONS_PERSONALISATION = "reject-reasons";
    private static final String LINK_TO_SERVICE_PERSONALISATION = "link-to-service";

    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String REFERENCE_ID_MESSAGE = "Reference ID does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    @Autowired
    private NotifyConfigProperties notifyConfigProperties;

    @Autowired
    private MediaAccountRejectionEmailGenerator emailGenerator;

    @Test
    void testBuildMediaAccountRejectionEmail() {
        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();

        MediaRejectionEmail rejectionEmail = new MediaRejectionEmail(FULL_NAME, EMAIL,REASONS);
        MediaAccountRejectionEmailData emailData = new MediaAccountRejectionEmailData(rejectionEmail);

        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(MEDIA_USER_REJECTION_EMAIL.getTemplate());

        softly.assertThat(result.getReferenceId())
            .as(REFERENCE_ID_MESSAGE)
            .isNotNull();

        Map<String, Object> personalisation = result.getPersonalisation();

        softly.assertThat(personalisation.get(FULL_NAME_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(FULL_NAME);

        softly.assertThat(personalisation.get(REJECTION_REASONS_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(List.of("reason\n^description"));

        softly.assertThat(personalisation.get(LINK_TO_SERVICE_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getStartPageLink() + "/create-media-account");

        softly.assertAll();
    }
}
