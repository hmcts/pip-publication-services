package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

@Data
public class LocationSubscriptionDeletion {

    @NotNull
    String locationName;

    @NotNull
    List<String> subscriberEmails = new ArrayList<>();
}
