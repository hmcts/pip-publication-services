package uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.service.notify.RetentionPeriodDuration;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;

@Getter
@Setter
@NoArgsConstructor
public class MiDataReportingEmailData extends EmailData {
    private byte[] excel;
    private RetentionPeriodDuration fileRetentionWeeks;
    private String envName;

    public MiDataReportingEmailData(String email, byte[] excel, int fileRetentionWeeks, String envName) {
        super(email);
        this.excel = excel == null ? new byte[0] : Arrays.copyOf(excel, excel.length);
        this.fileRetentionWeeks = new RetentionPeriodDuration(fileRetentionWeeks, ChronoUnit.WEEKS);
        this.envName = envName;
    }
}
