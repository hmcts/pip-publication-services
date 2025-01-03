package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.BatchEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.LocationSubscriptionDeletionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.BatchEmailGenerator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.DELETE_LOCATION_SUBSCRIPTION;

@Service
/**
 * Generate the location subscription email with personalisation for GOV.UK Notify template.
 */
public class LocationSubscriptionDeletionEmailGenerator extends BatchEmailGenerator {
    @Override
    public List<EmailToSend> buildEmail(BatchEmailData email, PersonalisationLinks personalisationLinks) {
        LocationSubscriptionDeletionEmailData emailData = (LocationSubscriptionDeletionEmailData) email;
        return generateEmail(emailData.getEmails(), DELETE_LOCATION_SUBSCRIPTION.getTemplate(),
                             buildEmailPersonalisation(emailData), email.getReferenceId());
    }

    private Map<String, Object> buildEmailPersonalisation(LocationSubscriptionDeletionEmailData emailData) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("location-name", emailData.getLocationName());

        return personalisation;
    }
}
