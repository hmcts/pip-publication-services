package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

@Data
public class AdminActionEmail {
    @Email
    @NotNull
    private String email;

    @NotNull
    private String name;

    @NotNull
    private String changeType;

    @NotNull
    private String actionResult;

    private String additionalInformation;
}
