package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.EmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.InactiveUserNotificationEmailBody;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.INACTIVE_USER_NOTIFICATION_EMAIL_AAD;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.INACTIVE_USER_NOTIFICATION_EMAIL_CFT;

@Service
public class InactiveUserNotificationEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailBody email, PersonalisationLinks personalisationLinks) {
        InactiveUserNotificationEmailBody emailBody = (InactiveUserNotificationEmailBody) email;
        Templates emailTemplate = "PI_AAD".equals(emailBody.getUserProvenance())
            ? INACTIVE_USER_NOTIFICATION_EMAIL_AAD
            : INACTIVE_USER_NOTIFICATION_EMAIL_CFT;

        return generateEmail(emailBody.getEmail(), emailTemplate.getTemplate(),
                             buildEmailPersonalisation(emailBody, personalisationLinks));
    }

    private Map<String, Object> buildEmailPersonalisation(InactiveUserNotificationEmailBody emailBody,
                                                          PersonalisationLinks personalisationLinks) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("full_name", emailBody.getFullName());
        personalisation.put("last_signed_in_date", emailBody.getLastSignedInDate());
        personalisation.put("sign_in_page_link", personalisationLinks.getAadAdminSignInPageLink());
        personalisation.put("cft_sign_in_link", personalisationLinks.getCftSignInPageLink());
        return personalisation;
    }
}
