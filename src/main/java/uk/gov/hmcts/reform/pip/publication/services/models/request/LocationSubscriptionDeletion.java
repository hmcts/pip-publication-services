package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
public class LocationSubscriptionDeletion {

    @NotNull
    String locationName;

    @NotNull
    List<String> subscriberEmails = new ArrayList<>();
}
