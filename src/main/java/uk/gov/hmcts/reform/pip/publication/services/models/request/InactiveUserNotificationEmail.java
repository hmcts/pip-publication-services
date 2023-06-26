package uk.gov.hmcts.reform.pip.publication.services.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Value;

@Data
@Value
public class InactiveUserNotificationEmail {
    @Email
    @NotNull
    String email;

    @NotNull
    String fullName;

    @NotNull
    String userProvenance;

    @NotNull
    String lastSignedInDate;
}
