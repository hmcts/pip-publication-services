package uk.gov.hmcts.reform.pip.publication.services.models.emailbody;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UnidentifiedBlobEmailBody extends EmailBody {
    private List<NoMatchArtefact> noMatchArtefacts;
    private String envName;

    public UnidentifiedBlobEmailBody(String email, List<NoMatchArtefact> noMatchArtefacts, String envName) {
        super(email);
        this.noMatchArtefacts = noMatchArtefacts;
        this.envName = envName;
    }
}
