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
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.InactiveUserNotificationEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;

import java.util.Map;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.INACTIVE_USER_NOTIFICATION_EMAIL_AAD;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.INACTIVE_USER_NOTIFICATION_EMAIL_CFT;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.INACTIVE_USER_NOTIFICATION_EMAIL_CRIME;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
class InactiveUserNotificationEmailGeneratorTest extends RedisConfigurationTestBase {
    private static final String EMAIL = "test@testing.com";
    private static final String FULL_NAME = "Full name";
    private static final String AAD_USER_PROVENANCE = "PI_AAD";
    private static final String CFT_IDAM_USER_PROVENANCE = "CFT_IDAM";
    private static final String CRIME_IDAM_USER_PROVENANCE = "CRIME_IDAM";
    private static final String LAST_SIGN_IN_DATE = "01/05/2024";

    private static final String FULL_NAME_PERSONALISATION = "full_name";
    private static final String LAST_SIGN_IN_DATE_PERSONALISATION = "last_signed_in_date";
    private static final String AAD_SIGN_IN_PAGE_LINK = "sign_in_page_link";
    private static final String CFT_SIGN_IN_PAGE_LINK = "cft_sign_in_link";
    private static final String CRIME_SIGN_IN_PAGE_LINK = "crime_sign_in_link";

    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    private PersonalisationLinks personalisationLinks;

    @Autowired
    private NotifyConfigProperties notifyConfigProperties;

    @Autowired
    private InactiveUserNotificationEmailGenerator emailGenerator;

    @BeforeEach
    void setup() {
        personalisationLinks = notifyConfigProperties.getLinks();
    }

    @Test
    void testBuildInactiveAadUserNotificationEmail() {
        InactiveUserNotificationEmail notificationEmail = new InactiveUserNotificationEmail(
            EMAIL, FULL_NAME, AAD_USER_PROVENANCE, LAST_SIGN_IN_DATE
        );
        InactiveUserNotificationEmailData emailData = new InactiveUserNotificationEmailData(notificationEmail);

        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(INACTIVE_USER_NOTIFICATION_EMAIL_AAD.getTemplate());

        verifyPersonalisation(softly, result.getPersonalisation());
        softly.assertAll();
    }

    @Test
    void testBuildInactiveCftUserNotificationEmail() {
        InactiveUserNotificationEmail notificationEmail = new InactiveUserNotificationEmail(
            EMAIL, FULL_NAME, CFT_IDAM_USER_PROVENANCE, LAST_SIGN_IN_DATE
        );
        InactiveUserNotificationEmailData emailData = new InactiveUserNotificationEmailData(notificationEmail);

        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(INACTIVE_USER_NOTIFICATION_EMAIL_CFT.getTemplate());

        verifyPersonalisation(softly, result.getPersonalisation());
        softly.assertAll();
    }

    @Test
    void testBuildInactiveCrimeUserNotificationEmail() {
        InactiveUserNotificationEmail notificationEmail = new InactiveUserNotificationEmail(
            EMAIL, FULL_NAME, CRIME_IDAM_USER_PROVENANCE, LAST_SIGN_IN_DATE
        );
        InactiveUserNotificationEmailData emailData = new InactiveUserNotificationEmailData(notificationEmail);

        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(INACTIVE_USER_NOTIFICATION_EMAIL_CRIME.getTemplate());

        verifyPersonalisation(softly, result.getPersonalisation());
        softly.assertAll();
    }

    private void verifyPersonalisation(SoftAssertions softly, Map<String, Object> personalisation) {
        softly.assertThat(personalisation.get(FULL_NAME_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(FULL_NAME);

        softly.assertThat(personalisation.get(LAST_SIGN_IN_DATE_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(LAST_SIGN_IN_DATE);

        softly.assertThat(personalisation.get(AAD_SIGN_IN_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getAadAdminSignInPageLink());

        softly.assertThat(personalisation.get(CFT_SIGN_IN_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getCftSignInPageLink());

        softly.assertThat(personalisation.get(CRIME_SIGN_IN_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(personalisationLinks.getCrimeSignInPageLink());
    }
}
