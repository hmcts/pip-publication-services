package uk.gov.hmcts.reform.pip.publication.services.models.emaildata;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;

@Getter
@Setter
@NoArgsConstructor
public class MediaDuplicatedAccountEmailData extends EmailData {
    private String fullName;

    public MediaDuplicatedAccountEmailData(DuplicatedMediaEmail duplicatedMediaEmail) {
        super(duplicatedMediaEmail.getEmail());
        this.fullName = duplicatedMediaEmail.getFullName();
    }
}
