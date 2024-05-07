package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.reporting;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting.UnidentifiedBlobEmailData;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.EmailGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.models.Environments.convertEnvironmentName;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.BAD_BLOB_EMAIL;

@Service
/**
 * Generate the unidentified blob email with personalisation for GOV.UK Notify template.
 */
public class UnidentifiedBlobEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailData email, PersonalisationLinks personalisationLinks) {
        UnidentifiedBlobEmailData emailData = (UnidentifiedBlobEmailData) email;
        return generateEmail(emailData.getEmail(), BAD_BLOB_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailData));
    }

    private Map<String, Object> buildEmailPersonalisation(UnidentifiedBlobEmailData emailData) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        List<String> listOfUnmatched = new ArrayList<>();

        emailData.getNoMatchArtefacts()
            .forEach(noMatchArtefact -> listOfUnmatched.add(
                String.format("%s - %s (%s)", noMatchArtefact.getLocationId(), noMatchArtefact.getProvenance(),
                              noMatchArtefact.getArtefactId())
            ));

        personalisation.put("array_of_ids", listOfUnmatched);
        personalisation.put("env_name", convertEnvironmentName(emailData.getEnvName()));
        return personalisation;
    }
}
