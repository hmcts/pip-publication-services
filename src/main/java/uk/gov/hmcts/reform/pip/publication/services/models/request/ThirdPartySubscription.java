package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotNull;

@Data
public class ThirdPartySubscription {

    @NotNull
    String apiDestination;

    @NotNull
    List<UUID> artefactIds;
}
