package uk.gov.hmcts.reform.pip.publication.services.models.emaildata;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;

@Getter
@Setter
@NoArgsConstructor
public class InactiveUserNotificationEmailData extends EmailData {
    private String fullName;
    private String userProvenance;
    private String lastSignedInDate;

    public InactiveUserNotificationEmailData(InactiveUserNotificationEmail notificationEmail) {
        super(notificationEmail.getEmail());
        this.fullName = notificationEmail.getFullName();
        this.userProvenance = notificationEmail.getUserProvenance();
        this.lastSignedInDate = notificationEmail.getLastSignedInDate();
    }
}
