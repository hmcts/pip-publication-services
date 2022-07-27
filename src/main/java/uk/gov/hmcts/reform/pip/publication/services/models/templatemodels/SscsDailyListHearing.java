package uk.gov.hmcts.reform.pip.publication.services.models.templatemodels;

import lombok.Data;

@Data
public class SscsDailyListHearing {
    String hearingTime;
    String appealRef;
    String tribunalType;
    String appellant;
    String respondent;
    String judiciary;
}
