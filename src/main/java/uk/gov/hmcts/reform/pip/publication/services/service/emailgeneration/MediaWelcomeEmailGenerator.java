package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.EmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.MediaWelcomeEmailBody;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.EXISTING_USER_WELCOME_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_NEW_ACCOUNT_SETUP;

@Service
public class MediaWelcomeEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailBody email, PersonalisationLinks personalisationLinks) {
        MediaWelcomeEmailBody emailBody = (MediaWelcomeEmailBody) email;
        Templates emailTemplate = emailBody.isExisting()
            ? EXISTING_USER_WELCOME_EMAIL
            : MEDIA_NEW_ACCOUNT_SETUP;

        return generateEmail(emailBody.getEmail(), emailTemplate.getTemplate(),
                             buildEmailPersonalisation(emailBody, personalisationLinks));
    }

    private Map<String, Object> buildEmailPersonalisation(MediaWelcomeEmailBody emailBody,
                                                          PersonalisationLinks personalisationLinks) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("full_name", emailBody.getFullName());
        personalisation.put("forgot_password_process_link", personalisationLinks.getAadPwResetLinkMedia());
        personalisation.put("subscription_page_link", personalisationLinks.getSubscriptionPageLink());
        personalisation.put("start_page_link", personalisationLinks.getStartPageLink());
        personalisation.put("gov_guidance_page", personalisationLinks.getGovGuidancePageLink());
        return personalisation;
    }
}
