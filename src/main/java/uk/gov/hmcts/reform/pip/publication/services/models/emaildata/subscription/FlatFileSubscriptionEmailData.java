package uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.service.notify.RetentionPeriodDuration;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;

@Getter
@Setter
@NoArgsConstructor
public class FlatFileSubscriptionEmailData extends EmailData {
    private Artefact artefact;
    private String locationName;
    private byte[] artefactFlatFile;
    private RetentionPeriodDuration fileRetentionWeeks;

    public FlatFileSubscriptionEmailData(SubscriptionEmail subscriptionEmail, Artefact artefact, String locationName,
                                         byte[] artefactFlatFile, int fileRetentionWeeks, String referenceId) {
        super(subscriptionEmail.getEmail(), referenceId);
        this.artefact = artefact;
        this.locationName = locationName;
        this.artefactFlatFile = artefactFlatFile == null ? new byte[0]
            : Arrays.copyOf(artefactFlatFile, artefactFlatFile.length);
        this.fileRetentionWeeks = new RetentionPeriodDuration(fileRetentionWeeks, ChronoUnit.WEEKS);
    }
}
