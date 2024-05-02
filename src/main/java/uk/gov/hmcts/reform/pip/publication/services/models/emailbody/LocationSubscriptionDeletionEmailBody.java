package uk.gov.hmcts.reform.pip.publication.services.models.emailbody;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;

@Getter
@Setter
@NoArgsConstructor
public class LocationSubscriptionDeletionEmailBody extends BatchEmailBody {
    private String locationName;

    public LocationSubscriptionDeletionEmailBody(LocationSubscriptionDeletion locationSubscriptionDeletion) {
        super(locationSubscriptionDeletion.getSubscriberEmails());
        this.locationName = locationSubscriptionDeletion.getLocationName();
    }
}
