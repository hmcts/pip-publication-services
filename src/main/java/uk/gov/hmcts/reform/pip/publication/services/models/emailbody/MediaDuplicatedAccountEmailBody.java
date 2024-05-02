package uk.gov.hmcts.reform.pip.publication.services.models.emailbody;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;

@Getter
@Setter
@NoArgsConstructor
public class MediaDuplicatedAccountEmailBody extends EmailBody {
    private String fullName;

    public MediaDuplicatedAccountEmailBody(DuplicatedMediaEmail duplicatedMediaEmail) {
        super(duplicatedMediaEmail.getEmail());
        this.fullName = duplicatedMediaEmail.getFullName();
    }
}
