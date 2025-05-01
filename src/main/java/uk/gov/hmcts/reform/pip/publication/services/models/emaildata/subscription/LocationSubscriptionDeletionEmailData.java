package uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.BatchEmailData;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class LocationSubscriptionDeletionEmailData extends BatchEmailData {
    private String locationName;

    public LocationSubscriptionDeletionEmailData(List<String> subscriberEmails, String locationName,
                                                 String referenceId) {
        super(subscriberEmails, referenceId);
        this.locationName = locationName;
    }
}
