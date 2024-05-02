package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.EmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.OtpEmailBody;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.OTP_EMAIL;

@Service
public class OtpEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailBody email, PersonalisationLinks personalisationLinks) {
        OtpEmailBody emailBody = (OtpEmailBody) email;
        return generateEmail(emailBody.getEmail(), OTP_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailBody));
    }

    private Map<String, Object> buildEmailPersonalisation(OtpEmailBody emailBody) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("otp", emailBody.getOtp());
        return personalisation;
    }
}
