package uk.gov.hmcts.reform.pip.publication.services.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Value;

@Data
@Value
public class WelcomeEmail {

    private String email;

    @JsonProperty("isExisting")
    private boolean isExisting;

    private String fullName;
}
