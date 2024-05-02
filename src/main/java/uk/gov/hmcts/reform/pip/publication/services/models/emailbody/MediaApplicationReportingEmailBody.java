package uk.gov.hmcts.reform.pip.publication.services.models.emailbody;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
@NoArgsConstructor
public class MediaApplicationReportingEmailBody extends EmailBody {
    private byte[] mediaApplicationsCsv;

    public MediaApplicationReportingEmailBody(String email, byte[] mediaApplicationsCsv) {
        super(email);
        this.mediaApplicationsCsv = mediaApplicationsCsv == null ? new byte[0]
            : Arrays.copyOf(mediaApplicationsCsv, mediaApplicationsCsv.length);
    }
}
