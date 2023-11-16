package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OtpEmail {
    private String otp;
    private String email;
}
