package uk.gov.hmcts.reform.pip.publication.services.models.emailbody;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;

@Getter
@Setter
@NoArgsConstructor
public class MediaWelcomeEmailBody extends EmailBody {
    private boolean isExisting;
    private String fullName;

    public MediaWelcomeEmailBody(WelcomeEmail welcomeEmail) {
        super(welcomeEmail.getEmail());
        this.isExisting = welcomeEmail.isExisting();
        this.fullName = welcomeEmail.getFullName();
    }
}
