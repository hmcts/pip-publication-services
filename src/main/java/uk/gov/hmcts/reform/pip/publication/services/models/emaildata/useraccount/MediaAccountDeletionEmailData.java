package uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaDeletionEmail;

@Getter
@Setter
@NoArgsConstructor
public class MediaAccountDeletionEmailData extends EmailData {
    private String fullName;
    private String reVerificationEmailDate;

    public MediaAccountDeletionEmailData(MediaDeletionEmail mediaDeletionEmail) {
        super(mediaDeletionEmail.getEmail());
        this.fullName = mediaDeletionEmail.getFullName();
        this.reVerificationEmailDate = mediaDeletionEmail.getReVerificationEmailDate();
    }
}
