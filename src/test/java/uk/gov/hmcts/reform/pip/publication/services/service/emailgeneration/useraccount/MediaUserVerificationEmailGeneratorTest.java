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
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.MediaUserVerificationEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;

import java.util.Map;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_USER_VERIFICATION_EMAIL;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
class MediaUserVerificationEmailGeneratorTest extends RedisConfigurationTestBase {
    private static final String EMAIL = "test@testing.com";
    private static final String FULL_NAME = "Full name";

    private static final String FULL_NAME_PERSONALISATION = "full_name";
    private static final String VERIFICATION_PAGE_LINK = "verification_page_link";

    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    @Autowired
    private NotifyConfigProperties notifyConfigProperties;

    @Autowired
    private MediaUserVerificationEmailGenerator emailGenerator;

    @Test
    void testBuildMediaUserVerificationEmail() {
        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();

        MediaVerificationEmail mediaEmail = new MediaVerificationEmail(FULL_NAME, EMAIL);
        MediaUserVerificationEmailData emailData = new MediaUserVerificationEmailData(mediaEmail);

        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(MEDIA_USER_VERIFICATION_EMAIL.getTemplate());

        Map<String, Object> personalisation = result.getPersonalisation();

        softly.assertThat(personalisation.get(FULL_NAME_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(FULL_NAME);

        softly.assertThat(personalisation.get(VERIFICATION_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getMediaVerificationPageLink());

        softly.assertAll();
    }
}
