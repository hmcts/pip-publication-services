package uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SscsDailyList;

import lombok.Data;

import java.util.List;

@Data
public class CourtRoom {
    String name;
    List<Sitting> listOfSittings;
}
