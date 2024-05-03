package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.MediaAccountRejectionEmailData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_USER_REJECTION_EMAIL;

@Service
/**
 * Generate the media account rejection email with personalisation for GOV.UK Notify template.
 */
public class MediaAccountRejectionEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailData email, PersonalisationLinks personalisationLinks) {
        MediaAccountRejectionEmailData emailData = (MediaAccountRejectionEmailData) email;
        return generateEmail(emailData.getEmail(), MEDIA_USER_REJECTION_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailData, personalisationLinks));
    }

    private Map<String, Object> buildEmailPersonalisation(MediaAccountRejectionEmailData emailData,
                                                          PersonalisationLinks personalisationLinks) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();

        personalisation.put("full-name", emailData.getFullName());

        personalisation.put("reject-reasons", formatReasons(emailData.getReasons()));
        personalisation.put("link-to-service", personalisationLinks.getStartPageLink()
            + "/create-media-account");

        return personalisation;
    }

    private static List<String> formatReasons(Map<String, List<String>> reasons) {
        List<String> reasonList = new ArrayList<>();
        reasons.forEach((key, value) -> reasonList.add(String.format("%s%n^%s", value.get(0), value.get(1))));
        return reasonList;
    }
}
