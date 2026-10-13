package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount;

import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.MediaAccountDeletionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.EmailGenerator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.INACTIVE_MEDIA_USER_DELETION_EMAIL;

public class MediaAccountDeletionEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailData email, PersonalisationLinks personalisationLinks) {
        MediaAccountDeletionEmailData emailData = (MediaAccountDeletionEmailData) email;
        return generateEmail(emailData, INACTIVE_MEDIA_USER_DELETION_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailData, personalisationLinks));
    }

    private Map<String, Object> buildEmailPersonalisation(MediaAccountDeletionEmailData emailData,
                                                          PersonalisationLinks personalisationLinks) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();

        personalisation.put("full-name", emailData.getFullName());

        personalisation.put("date", emailData.getReVerificationEmailDate());
        personalisation.put("link-to-service", personalisationLinks.getStartPageLink()
            + "/create-media-account");

        return personalisation;
    }
}
