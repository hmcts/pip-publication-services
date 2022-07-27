package uk.gov.hmcts.reform.pip.publication.services.models.templatemodels;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SscsDailyListCourtHouse {
    String name;
    List<SscsDailyListCourtRoom> listOfCourtRooms;
}
