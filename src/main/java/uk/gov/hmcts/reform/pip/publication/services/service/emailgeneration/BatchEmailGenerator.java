package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.BatchEmailBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class BatchEmailGenerator {
    public abstract List<EmailToSend> buildEmail(BatchEmailBody emailBody, PersonalisationLinks personalisationLinks);

    public List<EmailToSend> generateEmail(List<String> emails, String template,
                                            Map<String, Object> personalisation) {
        List<EmailToSend> createdEmails = new ArrayList<>();

        for (String email : emails) {
            String referenceId = UUID.randomUUID().toString();
            createdEmails.add(new EmailToSend(email, template, personalisation, referenceId));
        }

        return createdEmails;
    }
}
