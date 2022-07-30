package uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SscsDailyList;

import lombok.Data;

@Data
public class Hearing {
    String hearingTime;
    String appealRef;
    String tribunalType;
    String appellant;
    String respondent;
    String judiciary;
}
