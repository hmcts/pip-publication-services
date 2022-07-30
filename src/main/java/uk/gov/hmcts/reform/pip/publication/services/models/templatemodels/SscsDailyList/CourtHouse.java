package uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SscsDailyList;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourtHouse {
    String name;
    String phone;
    String email;
    List<CourtRoom> listOfCourtRooms;
}
