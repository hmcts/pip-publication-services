package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.Data;

@Data
public class OtpRequest {
    private String bearer;
    private String otp;
    private String email;
}
