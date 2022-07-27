package uk.gov.hmcts.reform.pip.publication.services.models.templatemodels;

import lombok.Data;

import java.util.List;

@Data
public class SscsDailyListCourtRoom {
    String name;
    List<SscsDailyListHearing> listOfHearings;
}
