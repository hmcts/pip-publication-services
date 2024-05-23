package uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;

@Getter
@Setter
@NoArgsConstructor
public class MediaUserVerificationEmailData extends EmailData {
    private String fullName;

    public MediaUserVerificationEmailData(MediaVerificationEmail mediaVerificationEmail) {
        super(mediaVerificationEmail.getEmail());
        this.fullName = mediaVerificationEmail.getFullName();
    }
}
