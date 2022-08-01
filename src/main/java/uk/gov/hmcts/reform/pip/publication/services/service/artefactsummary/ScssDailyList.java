package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.sscsdailylist.CourtHouse;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.sscsdailylist.CourtRoom;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.sscsdailylist.Hearing;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.sscsdailylist.Sitting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.Helpers.courtHouseBuilder;
import static uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.Helpers.handlePartiesScss;
import static uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.Helpers.hearingBuilder;
import static uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.Helpers.individualDetails;
import static uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.Helpers.safeGet;
import static uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.Helpers.safeGetNode;

@Service
public class ScssDailyList {


    private static final String INDIVIDUAL_DETAILS = "individualDetails";
    private static final String SESSION = "session";

    /**
     * sjp press parent method - iterates over session data. Routes to specific methods which handle offences and
     * judiciary roles.
     *
     * @param payload - json body.
     * @return String with final summary data.
     * @throws JsonProcessingException - jackson req.
     */
    public String artefactSummaryScssDailyList(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder();
        JsonNode node = new ObjectMapper().readTree(payload);
        List<CourtHouse> courtHouseList = new ArrayList<>();
        for (JsonNode courtHouse : node.get("courtLists")) {
            courtHouseList.add(courtHouseBuilder(courtHouse));
        }

        for (CourtHouse courtHouse : courtHouseList) {
            output.append('•').append(courtHouse.getName());
            for (CourtRoom courtRoom : courtHouse.getListOfCourtRooms()) {
                output.append('\n').append(courtRoom.getName());
                output.append('\n');
                for (Sitting sitting : courtRoom.getListOfSittings()) {
                    for (Hearing hearing : sitting.getListOfHearings()) {
                        output.append(hearing.getHearingTime()).append('\n');
                            output.append(hearing.getAppellant()).append('\n');
                            output.append(hearing.getRespondent()).append('\n');
                            output.append(hearing.getJudiciary()).append('\n');
                            output.append(sitting.getChannel()).append("\n");
                    }
                }
            }
        }
        return output.toString();
    }
}
