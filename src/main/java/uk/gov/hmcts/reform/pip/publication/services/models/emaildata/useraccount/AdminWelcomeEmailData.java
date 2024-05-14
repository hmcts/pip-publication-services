package uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;

@Getter
@Setter
@NoArgsConstructor
public class AdminWelcomeEmailData extends EmailData {
    private String forename;

    public AdminWelcomeEmailData(CreatedAdminWelcomeEmail welcomeEmail) {
        super(welcomeEmail.getEmail());
        this.forename = welcomeEmail.getForename();
    }
}
