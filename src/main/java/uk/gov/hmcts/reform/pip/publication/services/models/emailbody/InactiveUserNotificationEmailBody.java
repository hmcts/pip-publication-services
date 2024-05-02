package uk.gov.hmcts.reform.pip.publication.services.models.emailbody;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;

@Getter
@Setter
@NoArgsConstructor
public class InactiveUserNotificationEmailBody extends EmailBody {
    private String fullName;
    private String userProvenance;
    private String lastSignedInDate;

    public InactiveUserNotificationEmailBody(InactiveUserNotificationEmail notificationEmail) {
        super(notificationEmail.getEmail());
        this.fullName = notificationEmail.getFullName();
        this.userProvenance = notificationEmail.getUserProvenance();
        this.lastSignedInDate = notificationEmail.getLastSignedInDate();
    }
}
