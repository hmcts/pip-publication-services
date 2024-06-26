package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.InactiveUserNotificationEmailData;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.EmailGenerator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.INACTIVE_USER_NOTIFICATION_EMAIL_AAD;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.INACTIVE_USER_NOTIFICATION_EMAIL_CFT;

@Service
/**
 * Generate the inactive user notification email with personalisation for GOV.UK Notify template.
 */
public class InactiveUserNotificationEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailData email, PersonalisationLinks personalisationLinks) {
        InactiveUserNotificationEmailData emailData = (InactiveUserNotificationEmailData) email;
        Templates emailTemplate = UserProvenances.PI_AAD.name().equals(emailData.getUserProvenance())
            || UserProvenances.SSO.name().equals(emailData.getUserProvenance())
            ? INACTIVE_USER_NOTIFICATION_EMAIL_AAD
            : INACTIVE_USER_NOTIFICATION_EMAIL_CFT;

        return generateEmail(emailData.getEmail(), emailTemplate.getTemplate(),
                             buildEmailPersonalisation(emailData, personalisationLinks));
    }

    private Map<String, Object> buildEmailPersonalisation(InactiveUserNotificationEmailData emailData,
                                                          PersonalisationLinks personalisationLinks) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("full_name", emailData.getFullName());
        personalisation.put("last_signed_in_date", emailData.getLastSignedInDate());
        personalisation.put("sign_in_page_link", personalisationLinks.getAadAdminSignInPageLink());
        personalisation.put("cft_sign_in_link", personalisationLinks.getCftSignInPageLink());
        return personalisation;
    }
}
