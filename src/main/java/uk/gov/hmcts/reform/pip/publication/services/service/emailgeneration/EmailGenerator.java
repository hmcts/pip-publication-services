package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;

import java.util.Map;
import java.util.UUID;

public abstract class EmailGenerator {
    public abstract EmailToSend buildEmail(EmailData emailData,
                                           PersonalisationLinks personalisationLinks);

    public EmailToSend generateEmail(String email, String template, Map<String, Object> personalisation) {
        String referenceId = UUID.randomUUID().toString();
        return new EmailToSend(email, template, personalisation, referenceId);
    }
}
