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

/**
 * Generate the inactive user notification email with personalisation for GOV.UK Notify template.
 */
@Service
public class InactiveUserNotificationEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailData email, PersonalisationLinks personalisationLinks) {
        InactiveUserNotificationEmailData emailData = (InactiveUserNotificationEmailData) email;
        Templates emailTemplate = selectInactiveUserNotificationEmailTemplate(emailData.getUserProvenance());

        return generateEmail(emailData, emailTemplate.getTemplate(),
                             buildEmailPersonalisation(emailData, personalisationLinks));
    }

    private Templates selectInactiveUserNotificationEmailTemplate(String userProvenance) {
        if (UserProvenances.PI_AAD.name().equals(userProvenance)) {
            return Templates.INACTIVE_USER_NOTIFICATION_EMAIL_AAD;
        }

        return UserProvenances.CFT_IDAM.name().equals(userProvenance)
            ? Templates.INACTIVE_USER_NOTIFICATION_EMAIL_CFT
            : Templates.INACTIVE_USER_NOTIFICATION_EMAIL_CRIME;
    }

    private Map<String, Object> buildEmailPersonalisation(InactiveUserNotificationEmailData emailData,
                                                          PersonalisationLinks personalisationLinks) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("full_name", emailData.getFullName());
        personalisation.put("last_signed_in_date", emailData.getLastSignedInDate());
        personalisation.put("sign_in_page_link", personalisationLinks.getAadAdminSignInPageLink());
        personalisation.put("cft_sign_in_link", personalisationLinks.getCftSignInPageLink());
        personalisation.put("crime_sign_in_link", personalisationLinks.getCrimeSignInPageLink());
        return personalisation;
    }
}
