package uk.gov.hmcts.reform.pip.publication.services.models.emailbody;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.request.OtpEmail;

@Getter
@Setter
@NoArgsConstructor
public class OtpEmailBody extends EmailBody {
    private String otp;

    public OtpEmailBody(OtpEmail otpEmail) {
        super(otpEmail.getEmail());
        this.otp = otpEmail.getOtp();
    }
}
