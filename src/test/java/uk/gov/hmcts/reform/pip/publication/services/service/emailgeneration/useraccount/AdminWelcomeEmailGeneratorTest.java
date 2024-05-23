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
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.AdminWelcomeEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;

import java.util.Map;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.ADMIN_ACCOUNT_CREATION_EMAIL;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
class AdminWelcomeEmailGeneratorTest extends RedisConfigurationTestBase {
    private static final String EMAIL = "test@testing.com";
    private static final String NAME = "Name";

    private static final String FIRST_NAME_PERSONALISATION = "first_name";
    private static final String RESET_PASSWORD_LINK = "reset_password_link";
    private static final String ADMIN_DASHBOARD_LINK = "admin_dashboard_link";

    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    @Autowired
    private NotifyConfigProperties notifyConfigProperties;

    @Autowired
    private AdminWelcomeEmailGenerator emailGenerator;

    @Test
    void testBuildAdminAccountWelcomeEmail() {
        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();

        CreatedAdminWelcomeEmail welcomeEmail = new CreatedAdminWelcomeEmail(EMAIL, NAME, NAME);
        AdminWelcomeEmailData emailData = new AdminWelcomeEmailData(welcomeEmail);

        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(ADMIN_ACCOUNT_CREATION_EMAIL.getTemplate());

        Map<String, Object> personalisation = result.getPersonalisation();

        softly.assertThat(personalisation.get(FIRST_NAME_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(NAME);

        softly.assertThat(personalisation.get(RESET_PASSWORD_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getAadPwResetLinkAdmin());

        softly.assertThat(personalisation.get(ADMIN_DASHBOARD_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getAdminDashboardLink());

        softly.assertAll();
    }
}
