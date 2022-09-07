package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.Data;
import lombok.Value;

@Data
@Value
public class MediaVerificationEmail {
    String fullName;
    String email;
}
