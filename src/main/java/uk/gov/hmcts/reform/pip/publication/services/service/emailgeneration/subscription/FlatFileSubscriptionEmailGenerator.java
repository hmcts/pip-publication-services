package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.FlatFileSubscriptionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.EmailGenerator;
import uk.gov.service.notify.NotificationClientException;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL;
import static uk.gov.service.notify.NotificationClient.prepareUpload;

@Service
@Slf4j
@SuppressWarnings("PMD.PreserveStackTrace")
/**
 * Generate the flat file subscription email with personalisation for GOV.UK Notify template.
 */
public class FlatFileSubscriptionEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailData email, PersonalisationLinks personalisationLinks) {
        FlatFileSubscriptionEmailData emailData = (FlatFileSubscriptionEmailData) email;
        return generateEmail(emailData.getEmail(), MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailData, emailData.getArtefact(), personalisationLinks));
    }

    private Map<String, Object> buildEmailPersonalisation(FlatFileSubscriptionEmailData emailData, Artefact artefact,
                                                          PersonalisationLinks personalisationLinks) {
        try {
            Map<String, Object> personalisation = new ConcurrentHashMap<>();
            populateLocationPersonalisation(personalisation, emailData.getLocationName());

            personalisation.put("list_type", artefact.getListType().getFriendlyName());
            JSONObject uploadedFile = prepareUpload(emailData.getArtefactFlatFile(), false,
                                                    emailData.getFileRetentionWeeks());

            personalisation.put("link_to_file", uploadedFile);
            personalisation.put("start_page_link", personalisationLinks.getStartPageLink());
            personalisation.put("subscription_page_link", personalisationLinks.getSubscriptionPageLink());

            personalisation.put(
                "content_date",
                artefact.getContentDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
            );

            return personalisation;
        } catch (NotificationClientException e) {
            log.warn(writeLog(String.format(
                "Error adding attachment to flat file email %s. Artefact ID: %s",
                EmailHelper.maskEmail(emailData.getEmail()),
                emailData.getArtefactId()
            )));
            throw new NotifyException(e.getMessage());
        }
    }

    private void populateLocationPersonalisation(Map<String, Object> personalisation,String locationName) {
        personalisation.put("display_locations", locationName.isEmpty() ? "No" : "Yes");
        personalisation.put("locations", locationName);
    }
}
