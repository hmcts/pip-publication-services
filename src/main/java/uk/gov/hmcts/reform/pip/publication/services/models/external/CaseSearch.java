package uk.gov.hmcts.reform.pip.publication.services.models.external;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaseSearch {

    private String caseNumber;
    private String caseName;
    private String caseUrn;

}
