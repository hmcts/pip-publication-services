package uk.gov.hmcts.reform.pip.publication.services.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DuplicatedMediaEmail {
    @Email
    @NotNull
    String email;

    @NotNull
    String fullName;
}
