package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.BatchEmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.LocationSubscriptionDeletionEmailBody;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.DELETE_LOCATION_SUBSCRIPTION;

@Service
public class LocationSubscriptionDeletionEmailGenerator extends BatchEmailGenerator {
    @Override
    public List<EmailToSend> buildEmail(BatchEmailBody email, PersonalisationLinks personalisationLinks) {
        LocationSubscriptionDeletionEmailBody emailBody = (LocationSubscriptionDeletionEmailBody) email;
        return generateEmail(emailBody.getEmails(), DELETE_LOCATION_SUBSCRIPTION.getTemplate(),
                             buildEmailPersonalisation(emailBody));
    }

    private Map<String, Object> buildEmailPersonalisation(LocationSubscriptionDeletionEmailBody emailBody) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("location-name", emailBody.getLocationName());

        return personalisation;
    }
}
