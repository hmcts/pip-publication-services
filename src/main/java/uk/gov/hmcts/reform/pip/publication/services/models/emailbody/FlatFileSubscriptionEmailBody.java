package uk.gov.hmcts.reform.pip.publication.services.models.emailbody;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;

import java.util.Arrays;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class FlatFileSubscriptionEmailBody extends EmailBody {
    private UUID artefactId;
    private Artefact artefact;
    private String locationName;
    private byte[] artefactFlatFile;

    public FlatFileSubscriptionEmailBody(SubscriptionEmail subscriptionEmail, Artefact artefact, String locationName,
                                         byte[] artefactFlatFile) {
        super(subscriptionEmail.getEmail());
        this.artefactId = subscriptionEmail.getArtefactId();
        this.artefact = artefact;
        this.locationName = locationName;
        this.artefactFlatFile = artefactFlatFile == null ? new byte[0]
            : Arrays.copyOf(artefactFlatFile, artefactFlatFile.length);
    }
}
