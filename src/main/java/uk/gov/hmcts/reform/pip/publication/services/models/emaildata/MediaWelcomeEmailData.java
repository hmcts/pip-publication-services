package uk.gov.hmcts.reform.pip.publication.services.models.emaildata;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;

@Getter
@Setter
@NoArgsConstructor
public class MediaWelcomeEmailData extends EmailData {
    private boolean isExisting;
    private String fullName;

    public MediaWelcomeEmailData(WelcomeEmail welcomeEmail) {
        super(welcomeEmail.getEmail());
        this.isExisting = welcomeEmail.isExisting();
        this.fullName = welcomeEmail.getFullName();
    }
}
