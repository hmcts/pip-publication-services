package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.MediaDuplicatedAccountEmailData;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.EmailGenerator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_DUPLICATE_ACCOUNT_EMAIL;

@Service
/**
 * Generate the media duplicated account email with personalisation for GOV.UK Notify template.
 */
public class MediaDuplicatedAccountEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailData email, PersonalisationLinks personalisationLinks) {
        MediaDuplicatedAccountEmailData emailData = (MediaDuplicatedAccountEmailData) email;
        return generateEmail(emailData, MEDIA_DUPLICATE_ACCOUNT_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailData, personalisationLinks));
    }

    private Map<String, Object> buildEmailPersonalisation(MediaDuplicatedAccountEmailData emailData,
                                                          PersonalisationLinks personalisationLinks) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("full_name", emailData.getFullName());
        personalisation.put("sign_in_page_link", personalisationLinks.getAadSignInPageLink());
        return personalisation;
    }
}
