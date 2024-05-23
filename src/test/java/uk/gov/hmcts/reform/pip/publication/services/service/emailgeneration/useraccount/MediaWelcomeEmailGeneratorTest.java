package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.MediaWelcomeEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;

import java.util.Map;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.EXISTING_USER_WELCOME_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_NEW_ACCOUNT_SETUP;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
class MediaWelcomeEmailGeneratorTest extends RedisConfigurationTestBase {
    private static final String EMAIL = "test@testing.com";
    private static final String FULL_NAME = "Full name";

    private static final String FULL_NAME_PERSONALISATION = "full_name";
    private static final String FORGOT_PASSWORD_LINK = "forgot_password_process_link";
    private static final String SUBSCRIPTION_PAGE_LINK = "subscription_page_link";
    private static final String START_PAGE_LINK = "start_page_link";
    private static final String GOV_GUIDANCE_PAGE_LINK = "gov_guidance_page";

    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    private PersonalisationLinks personalisationLinks;

    @Autowired
    private NotifyConfigProperties notifyConfigProperties;

    @Autowired
    private MediaWelcomeEmailGenerator emailGenerator;

    @BeforeEach
    void setup() {
        personalisationLinks = notifyConfigProperties.getLinks();
    }

    @Test
    void testBuildNewMediaAccountWelcomeEmail() {
        WelcomeEmail welcomeEmail = new WelcomeEmail(EMAIL, false, FULL_NAME);
        MediaWelcomeEmailData emailData = new MediaWelcomeEmailData(welcomeEmail);

        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(MEDIA_NEW_ACCOUNT_SETUP.getTemplate());

        verifyPersonalisation(softly, result.getPersonalisation());

        softly.assertAll();
    }

    @Test
    void testBuildExistingMediaAccountWelcomeEmail() {
        WelcomeEmail welcomeEmail = new WelcomeEmail(EMAIL, true, FULL_NAME);
        MediaWelcomeEmailData emailData = new MediaWelcomeEmailData(welcomeEmail);

        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(EXISTING_USER_WELCOME_EMAIL.getTemplate());

        verifyPersonalisation(softly, result.getPersonalisation());
        softly.assertAll();
    }

    private void verifyPersonalisation(SoftAssertions softly, Map<String, Object> personalisation) {
        softly.assertThat(personalisation.get(FULL_NAME_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(FULL_NAME);

        softly.assertThat(personalisation.get(FORGOT_PASSWORD_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getAadPwResetLinkMedia());

        softly.assertThat(personalisation.get(SUBSCRIPTION_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getSubscriptionPageLink());

        softly.assertThat(personalisation.get(START_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getStartPageLink());

        softly.assertThat(personalisation.get(GOV_GUIDANCE_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getGovGuidancePageLink());
    }
}
