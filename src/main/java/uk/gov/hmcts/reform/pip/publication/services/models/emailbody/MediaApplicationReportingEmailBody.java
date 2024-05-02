package uk.gov.hmcts.reform.pip.publication.services.models.emailbody;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.service.notify.RetentionPeriodDuration;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;

@Getter
@Setter
@NoArgsConstructor
public class MediaApplicationReportingEmailBody extends EmailBody {
    private byte[] mediaApplicationsCsv;
    private RetentionPeriodDuration fileRetentionWeeks;
    private String envName;

    public MediaApplicationReportingEmailBody(String email, byte[] mediaApplicationsCsv, int fileRetentionWeeks,
                                              String envName) {
        super(email);
        this.mediaApplicationsCsv = mediaApplicationsCsv == null ? new byte[0]
            : Arrays.copyOf(mediaApplicationsCsv, mediaApplicationsCsv.length);
        this.fileRetentionWeeks = new RetentionPeriodDuration(fileRetentionWeeks, ChronoUnit.WEEKS);
        this.envName = envName;
    }
}
