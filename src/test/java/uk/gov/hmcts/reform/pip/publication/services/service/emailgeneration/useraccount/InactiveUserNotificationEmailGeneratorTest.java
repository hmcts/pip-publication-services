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
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.InactiveUserNotificationEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;

import java.util.Map;

import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.INACTIVE_USER_NOTIFICATION_EMAIL_CFT;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.INACTIVE_USER_NOTIFICATION_EMAIL_CRIME;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class InactiveUserNotificationEmailGeneratorTest {
    private static final String EMAIL = "test@testing.com";
    private static final String FULL_NAME = "Full name";
    private static final String CFT_IDAM_USER_PROVENANCE = "CFT_IDAM";
    private static final String CRIME_IDAM_USER_PROVENANCE = "CRIME_IDAM";
    private static final String LAST_SIGN_IN_DATE = "01/05/2024";

    private static final String FULL_NAME_PERSONALISATION = "full_name";
    private static final String LAST_SIGN_IN_DATE_PERSONALISATION = "last_signed_in_date";
    private static final String CFT_SIGN_IN_PAGE_LINK = "cft_sign_in_link";
    private static final String CRIME_SIGN_IN_PAGE_LINK = "crime_sign_in_link";

    private static final String CFT_SIGN_IN_PAGE_LINK_ADDRESS = "http://www.test-link2.com";
    private static final String CRIME_SIGN_IN_PAGE_LINK_ADDRESS = "http://www.test-link3.com";

    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String REFERENCE_ID_MESSAGE = "Reference ID does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    @Mock
    private PersonalisationLinks personalisationLinks;

    @InjectMocks
    private InactiveUserNotificationEmailGenerator emailGenerator;

    @BeforeEach
    void setup() {
        when(personalisationLinks.getCftSignInPageLink()).thenReturn(CFT_SIGN_IN_PAGE_LINK_ADDRESS);
        when(personalisationLinks.getCrimeSignInPageLink()).thenReturn(CRIME_SIGN_IN_PAGE_LINK_ADDRESS);
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

        softly.assertThat(result.getReferenceId())
            .as(REFERENCE_ID_MESSAGE)
            .isNotNull();

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

        softly.assertThat(personalisation.get(LAST_SIGN_IN_DATE_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(LAST_SIGN_IN_DATE);

        softly.assertThat(personalisation.get(CFT_SIGN_IN_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(CFT_SIGN_IN_PAGE_LINK_ADDRESS);

        softly.assertThat(personalisation.get(CRIME_SIGN_IN_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(CRIME_SIGN_IN_PAGE_LINK_ADDRESS);
    }
}
