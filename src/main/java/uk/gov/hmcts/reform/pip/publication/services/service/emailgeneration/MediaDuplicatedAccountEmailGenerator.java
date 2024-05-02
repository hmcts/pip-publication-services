package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.EmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.MediaDuplicatedAccountEmailBody;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_DUPLICATE_ACCOUNT_EMAIL;

@Service
public class MediaDuplicatedAccountEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailBody email, PersonalisationLinks personalisationLinks) {
        MediaDuplicatedAccountEmailBody emailBody = (MediaDuplicatedAccountEmailBody) email;
        return generateEmail(emailBody.getEmail(), MEDIA_DUPLICATE_ACCOUNT_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailBody, personalisationLinks));
    }

    private Map<String, Object> buildEmailPersonalisation(MediaDuplicatedAccountEmailBody emailBody,
                                                          PersonalisationLinks personalisationLinks) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("full_name", emailBody.getFullName());
        personalisation.put("sign_in_page_link", personalisationLinks.getAadSignInPageLink());
        return personalisation;
    }
}
