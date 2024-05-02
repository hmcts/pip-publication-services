package uk.gov.hmcts.reform.pip.publication.services.models.emailbody;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;

@Getter
@Setter
@NoArgsConstructor
public class AdminWelcomeEmailBody extends EmailBody {
    private String forename;

    public AdminWelcomeEmailBody(CreatedAdminWelcomeEmail welcomeEmail) {
        super(welcomeEmail.getEmail());
        this.forename = welcomeEmail.getForename();
    }
}
