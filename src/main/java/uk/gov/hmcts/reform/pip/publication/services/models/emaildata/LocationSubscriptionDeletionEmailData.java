package uk.gov.hmcts.reform.pip.publication.services.models.emaildata;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;

@Getter
@Setter
@NoArgsConstructor
public class LocationSubscriptionDeletionEmailData extends BatchEmailData {
    private String locationName;

    public LocationSubscriptionDeletionEmailData(LocationSubscriptionDeletion locationSubscriptionDeletion) {
        super(locationSubscriptionDeletion.getSubscriberEmails());
        this.locationName = locationSubscriptionDeletion.getLocationName();
    }
}
