package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

@Data
public class DuplicatedMediaEmail {
    @Email
    @NotNull
    String email;

    @NotNull
    String fullName;
}
