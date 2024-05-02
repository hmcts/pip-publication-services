package uk.gov.hmcts.reform.pip.publication.services.models.emailbody;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;

@Getter
@Setter
@NoArgsConstructor
public class MediaUserVerificationEmailBody extends EmailBody {
    private String fullName;

    public MediaUserVerificationEmailBody(MediaVerificationEmail mediaVerificationEmail) {
        super(mediaVerificationEmail.getEmail());
        this.fullName = mediaVerificationEmail.getFullName();
    }
}
