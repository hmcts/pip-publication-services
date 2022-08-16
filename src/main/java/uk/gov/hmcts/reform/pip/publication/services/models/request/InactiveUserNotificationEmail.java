package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.Data;
import lombok.Value;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

@Data
@Value
public class InactiveUserNotificationEmail {
    @Email
    @NotNull
    String email;

    @NotNull
    String fullName;

    @NotNull
    String lastSignedInDate;
}
