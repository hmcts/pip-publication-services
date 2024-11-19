package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;

import java.util.Map;

public abstract class EmailGenerator {
    public abstract EmailToSend buildEmail(EmailData emailData, PersonalisationLinks personalisationLinks);

    public EmailToSend generateEmail(EmailData emailData, String template, Map<String, Object> personalisation) {
        return new EmailToSend(emailData.getEmail(), template, personalisation, emailData.getReferenceId());
    }
}
