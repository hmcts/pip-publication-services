package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.EmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.MediaUserVerificationEmailBody;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_USER_VERIFICATION_EMAIL;

@Service
public class MediaUserVerificationEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailBody email, PersonalisationLinks personalisationLinks) {
        MediaUserVerificationEmailBody emailBody = (MediaUserVerificationEmailBody) email;
        return generateEmail(emailBody.getEmail(), MEDIA_USER_VERIFICATION_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailBody, personalisationLinks));
    }

    private Map<String, Object> buildEmailPersonalisation(MediaUserVerificationEmailBody emailBody,
                                                          PersonalisationLinks personalisationLinks) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("full_name", emailBody.getFullName());
        personalisation.put("verification_page_link", personalisationLinks.getMediaVerificationPageLink());
        return personalisation;
    }
}
