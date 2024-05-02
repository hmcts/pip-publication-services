package uk.gov.hmcts.reform.pip.publication.services.models.emaildata;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaRejectionEmail;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class MediaAccountRejectionEmailData extends EmailData {
    private String fullName;
    private Map<String, List<String>> reasons;

    public MediaAccountRejectionEmailData(MediaRejectionEmail mediaRejectionEmail) {
        super(mediaRejectionEmail.getEmail());
        this.fullName = mediaRejectionEmail.getFullName();
        this.reasons = mediaRejectionEmail.getReasons();
    }
}
