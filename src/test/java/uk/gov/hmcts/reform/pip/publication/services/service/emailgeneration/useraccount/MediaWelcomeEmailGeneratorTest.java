package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.MediaWelcomeEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;

import java.util.Map;

import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.EXISTING_USER_WELCOME_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_NEW_ACCOUNT_SETUP;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MediaWelcomeEmailGeneratorTest {
    private static final String EMAIL = "test@testing.com";
    private static final String FULL_NAME = "Full name";

    private static final String FULL_NAME_PERSONALISATION = "full_name";
    private static final String FORGOT_PASSWORD_LINK = "forgot_password_process_link";
    private static final String SUBSCRIPTION_PAGE_LINK = "subscription_page_link";
    private static final String START_PAGE_LINK = "start_page_link";
    private static final String GOV_GUIDANCE_PAGE_LINK = "gov_guidance_page";

    private static final String FORGOT_PASSWORD_LINK_ADDRESS = "http://www.test-link1.com";
    private static final String SUBSCRIPTION_PAGE_LINK_ADDRESS = "http://www.test-link2.com";
    private static final String START_PAGE_LINK_ADDRESS = "http://www.test-link3.com";
    private static final String GOV_GUIDANCE_PAGE_LINK_ADDRESS = "http://www.test-link4.com";

    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String REFERENCE_ID_MESSAGE = "Reference ID does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    @Mock
    private PersonalisationLinks personalisationLinks;

    @InjectMocks
    private MediaWelcomeEmailGenerator emailGenerator;

    @BeforeEach
    public void setup() {
        when(personalisationLinks.getAadPwResetLinkMedia()).thenReturn(FORGOT_PASSWORD_LINK_ADDRESS);
        when(personalisationLinks.getSubscriptionPageLink()).thenReturn(SUBSCRIPTION_PAGE_LINK_ADDRESS);
        when(personalisationLinks.getStartPageLink()).thenReturn(START_PAGE_LINK_ADDRESS);
        when(personalisationLinks.getGovGuidancePageLink()).thenReturn(GOV_GUIDANCE_PAGE_LINK_ADDRESS);
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

        softly.assertThat(result.getReferenceId())
            .as(REFERENCE_ID_MESSAGE)
            .isNotNull();

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

        softly.assertThat(result.getReferenceId())
            .as(REFERENCE_ID_MESSAGE)
            .isNotNull();

        verifyPersonalisation(softly, result.getPersonalisation());
        softly.assertAll();
    }

    private void verifyPersonalisation(SoftAssertions softly, Map<String, Object> personalisation) {
        softly.assertThat(personalisation.get(FULL_NAME_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(FULL_NAME);

        softly.assertThat(personalisation.get(FORGOT_PASSWORD_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(FORGOT_PASSWORD_LINK_ADDRESS);

        softly.assertThat(personalisation.get(SUBSCRIPTION_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(SUBSCRIPTION_PAGE_LINK_ADDRESS);

        softly.assertThat(personalisation.get(START_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(START_PAGE_LINK_ADDRESS);

        softly.assertThat(personalisation.get(GOV_GUIDANCE_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(GOV_GUIDANCE_PAGE_LINK_ADDRESS);
    }
}
