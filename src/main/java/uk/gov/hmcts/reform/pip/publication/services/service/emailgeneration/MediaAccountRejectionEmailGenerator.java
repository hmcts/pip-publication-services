package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.EmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.MediaAccountRejectionEmailBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_USER_REJECTION_EMAIL;

@Service
public class MediaAccountRejectionEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailBody email, PersonalisationLinks personalisationLinks) {
        MediaAccountRejectionEmailBody emailBody = (MediaAccountRejectionEmailBody) email;
        return generateEmail(emailBody.getEmail(), MEDIA_USER_REJECTION_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailBody, personalisationLinks));
    }

    private Map<String, Object> buildEmailPersonalisation(MediaAccountRejectionEmailBody emailBody,
                                                          PersonalisationLinks personalisationLinks) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();

        personalisation.put("full-name", emailBody.getFullName());

        personalisation.put("reject-reasons", formatReasons(emailBody.getReasons()));
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
