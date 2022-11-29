package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.Data;
import lombok.Value;

@Data
@Value
public class SystemAdminActionEmail {

    private String email;
    private String name;
    private String changeType;
    private String actionResult;
    private String additionalInformation;
}
