package uk.gov.hmcts.reform.pip.publication.services.models.external;

import lombok.Getter;
import lombok.Setter;

/**
 * This model captures the case search details that are extracted from the blob.
 * This can then be used in the subscriptions email, mapping case number to case name.
 */
@Getter
@Setter
public class CaseSearch {

    private String caseNumber;
    private String caseName;
    private String caseUrn;

}
