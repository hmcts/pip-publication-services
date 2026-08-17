package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.Data;
import lombok.Value;

@Data
@Value
public class WelcomeEmail {

    private String email;
    private boolean isExisting;
    private String fullName;
}
