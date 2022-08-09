package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.Data;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;

import javax.validation.constraints.NotNull;

@Data
public class ThirdPartySubscriptionArtefact {
    @NotNull
    String apiDestination;

    @NotNull
    Artefact artefact;
}
