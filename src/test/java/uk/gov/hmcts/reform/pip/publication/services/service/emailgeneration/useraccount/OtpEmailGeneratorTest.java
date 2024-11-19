package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.OtpEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.OtpEmail;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;

import java.util.Map;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.OTP_EMAIL;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
class OtpEmailGeneratorTest extends RedisConfigurationTestBase {
    private static final String EMAIL = "test@testing.com";
    private static final String OTP = "123456";
    private static final String OTP_PERSONALISATION = "otp";

    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String REFERENCE_ID_MESSAGE = "Reference ID does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    @Autowired
    private NotifyConfigProperties notifyConfigProperties;

    @Autowired
    private OtpEmailGenerator emailGenerator;

    @Test
    void testBuildOtpEmail() {
        OtpEmail otpEmail = new OtpEmail(OTP, EMAIL);
        OtpEmailData emailData = new OtpEmailData(otpEmail);

        EmailToSend result = emailGenerator.buildEmail(emailData, notifyConfigProperties.getLinks());

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(OTP_EMAIL.getTemplate());

        softly.assertThat(result.getReferenceId())
            .as(REFERENCE_ID_MESSAGE)
            .isNotNull();

        Map<String, Object> personalisation = result.getPersonalisation();

        softly.assertThat(personalisation.get(OTP_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(OTP);

        softly.assertAll();
    }
}
