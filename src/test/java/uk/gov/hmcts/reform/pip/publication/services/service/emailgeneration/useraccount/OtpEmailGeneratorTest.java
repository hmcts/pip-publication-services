package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.OtpEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.OtpEmail;

import java.util.Map;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.OTP_EMAIL;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class OtpEmailGeneratorTest {
    private static final String EMAIL = "test@testing.com";
    private static final String OTP = "123456";
    private static final String OTP_PERSONALISATION = "otp";

    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String REFERENCE_ID_MESSAGE = "Reference ID does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    @Mock
    private PersonalisationLinks personalisationLinks;

    @InjectMocks
    private OtpEmailGenerator emailGenerator;

    @Test
    void testBuildOtpEmail() {
        OtpEmail otpEmail = new OtpEmail(OTP, EMAIL);
        OtpEmailData emailData = new OtpEmailData(otpEmail);

        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

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
