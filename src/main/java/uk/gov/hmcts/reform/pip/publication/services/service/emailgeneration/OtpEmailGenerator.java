package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.OtpEmailData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.OTP_EMAIL;

@Service
/**
 * Generate the OTP email with personalisation for GOV.UK Notify template.
 */
public class OtpEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailData email, PersonalisationLinks personalisationLinks) {
        OtpEmailData emailData = (OtpEmailData) email;
        return generateEmail(emailData.getEmail(), OTP_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailData));
    }

    private Map<String, Object> buildEmailPersonalisation(OtpEmailData emailData) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("otp", emailData.getOtp());
        return personalisation;
    }
}
