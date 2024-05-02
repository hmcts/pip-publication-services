package uk.gov.hmcts.reform.pip.publication.services.models.emailbody;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaRejectionEmail;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class MediaAccountRejectionEmailBody extends EmailBody {
    private String fullName;
    private Map<String, List<String>> reasons;

    public MediaAccountRejectionEmailBody(MediaRejectionEmail mediaRejectionEmail) {
        super(mediaRejectionEmail.getEmail());
        this.fullName = mediaRejectionEmail.getFullName();
        this.reasons = mediaRejectionEmail.getReasons();
    }
}
