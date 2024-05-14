package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.MediaUserVerificationEmailData;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.EmailGenerator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_USER_VERIFICATION_EMAIL;

@Service
/**
 * Generate the media user verification email with personalisation for GOV.UK Notify template.
 */
public class MediaUserVerificationEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailData email, PersonalisationLinks personalisationLinks) {
        MediaUserVerificationEmailData emailData = (MediaUserVerificationEmailData) email;
        return generateEmail(emailData.getEmail(), MEDIA_USER_VERIFICATION_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailData, personalisationLinks));
    }

    private Map<String, Object> buildEmailPersonalisation(MediaUserVerificationEmailData emailData,
                                                          PersonalisationLinks personalisationLinks) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("full_name", emailData.getFullName());
        personalisation.put("verification_page_link", personalisationLinks.getMediaVerificationPageLink());
        return personalisation;
    }
}
