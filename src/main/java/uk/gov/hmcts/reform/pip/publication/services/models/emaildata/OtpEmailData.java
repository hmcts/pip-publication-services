package uk.gov.hmcts.reform.pip.publication.services.models.emaildata;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.request.OtpEmail;

@Getter
@Setter
@NoArgsConstructor
public class OtpEmailData extends EmailData {
    private String otp;

    public OtpEmailData(OtpEmail otpEmail) {
        super(otpEmail.getEmail());
        this.otp = otpEmail.getOtp();
    }
}
