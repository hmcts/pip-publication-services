package uk.gov.hmcts.reform.pip.publication.services.models.emailbody;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
@NoArgsConstructor
public class MiDataReportingEmailBody extends EmailBody {
    private byte[] excel;

    public MiDataReportingEmailBody(String email, byte[] excel) {
        super(email);
        this.excel = excel == null ? new byte[0] : Arrays.copyOf(excel, excel.length);
    }
}
