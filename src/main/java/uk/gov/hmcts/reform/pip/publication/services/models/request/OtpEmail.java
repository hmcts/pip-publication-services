package uk.gov.hmcts.reform.pip.publication.services.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OtpEmail {
    @NotNull
    @NotEmpty
    private String otp;

    @NotNull
    @Email
    private String email;
}
