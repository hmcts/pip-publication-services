package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.BatchEmailData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BatchEmailGenerator {
    public abstract List<EmailToSend> buildEmail(BatchEmailData emailData, PersonalisationLinks personalisationLinks);

    public List<EmailToSend> generateEmail(List<String> emails, String template, Map<String, Object> personalisation,
                                           String referenceId) {
        List<EmailToSend> createdEmails = new ArrayList<>();

        for (String email : emails) {
            createdEmails.add(new EmailToSend(email, template, personalisation, referenceId));
        }

        return createdEmails;
    }
}
