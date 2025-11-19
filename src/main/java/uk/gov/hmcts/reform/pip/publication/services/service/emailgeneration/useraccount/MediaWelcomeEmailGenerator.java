package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.MediaWelcomeEmailData;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.EmailGenerator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.EXISTING_USER_WELCOME_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_NEW_ACCOUNT_SETUP;

/**
 * Generate the media welcome email with personalisation for GOV.UK Notify template.
 */
@Service
public class MediaWelcomeEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailData email, PersonalisationLinks personalisationLinks) {
        MediaWelcomeEmailData emailData = (MediaWelcomeEmailData) email;
        Templates emailTemplate = emailData.isExisting()
            ? EXISTING_USER_WELCOME_EMAIL
            : MEDIA_NEW_ACCOUNT_SETUP;

        return generateEmail(emailData, emailTemplate.getTemplate(),
                             buildEmailPersonalisation(emailData, personalisationLinks));
    }

    private Map<String, Object> buildEmailPersonalisation(MediaWelcomeEmailData emailData,
                                                          PersonalisationLinks personalisationLinks) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("full_name", emailData.getFullName());
        personalisation.put("forgot_password_process_link", personalisationLinks.getAadPwResetLinkMedia());
        personalisation.put("subscription_page_link", personalisationLinks.getSubscriptionPageLink());
        personalisation.put("start_page_link", personalisationLinks.getStartPageLink());
        personalisation.put("gov_guidance_page", personalisationLinks.getGovGuidancePageLink());
        return personalisation;
    }
}
