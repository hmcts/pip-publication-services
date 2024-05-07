package uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.BatchEmailData;

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
