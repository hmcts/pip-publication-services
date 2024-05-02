package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.EmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.UnidentifiedBlobEmailBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.models.Environments.convertEnvironmentName;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.BAD_BLOB_EMAIL;

@Service
public class UnidentifiedBlobEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailBody email, PersonalisationLinks personalisationLinks) {
        UnidentifiedBlobEmailBody emailBody = (UnidentifiedBlobEmailBody) email;
        return generateEmail(emailBody.getEmail(), BAD_BLOB_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailBody));
    }

    private Map<String, Object> buildEmailPersonalisation(UnidentifiedBlobEmailBody emailBody) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        List<String> listOfUnmatched = new ArrayList<>();

        emailBody.getNoMatchArtefacts()
            .forEach(noMatchArtefact -> listOfUnmatched.add(
                String.format("%s - %s (%s)", noMatchArtefact.getLocationId(), noMatchArtefact.getProvenance(),
                              noMatchArtefact.getArtefactId())
            ));

        personalisation.put("array_of_ids", listOfUnmatched);
        personalisation.put("env_name", convertEnvironmentName(emailBody.getEnvName()));
        return personalisation;
    }
}
