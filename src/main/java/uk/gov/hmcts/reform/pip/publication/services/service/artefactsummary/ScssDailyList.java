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

import static uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.DataManipulation.courtHouseBuilder;

@Service
public class ScssDailyList {


    /**
     * parent method - first iterates through json file to build courthouse object list (with nested courtroom,
     * hearing and sitting objects within. Then iterate through those to produce final string output. Utilises a lot
     * of the same methods as the PDF.
     *
     * @param payload - json body.
     * @return String with final summary data.
     * @throws JsonProcessingException - jackson req.
     */
    public String artefactSummaryScssDailyList(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder(67);
        List<CourtHouse> courtHouseList = jsonParsePayload(payload);
        for (CourtHouse courtHouse : courtHouseList) {
            output.append("\n•").append(courtHouse.getName());
            output.append(courtRoomIterator(courtHouse.getListOfCourtRooms()));
        }
        return output.toString();
    }

    private String courtRoomIterator(List<CourtRoom> courtRoomList) {
        StringBuilder output = new StringBuilder();
        for (CourtRoom courtRoom : courtRoomList) {
            for (Sitting sitting : courtRoom.getListOfSittings()) {
                output.append('\n');
                Iterator<Hearing> hearingIterator = sitting.getListOfHearings().iterator();
                while (hearingIterator.hasNext()) {
                    Hearing hearing = hearingIterator.next();
                    output.append(courtRoom.getName()).append(", Time: ").append(hearing.getHearingTime());
                    output.append(hearingBuilder(hearing, sitting));
                    if (hearingIterator.hasNext()) {
                        output.append('\n');
                    }
                }
            }
        }
        return output.toString();
    }

    private List<CourtHouse> jsonParsePayload(String payload) throws JsonProcessingException {
        JsonNode node = new ObjectMapper().readTree(payload);
        List<CourtHouse> courtHouseList = new ArrayList<>();
        for (JsonNode courtHouse : node.get("courtLists")) {
            courtHouseList.add(courtHouseBuilder(courtHouse));
        }
        return courtHouseList;
    }

    private String hearingBuilder(Hearing hearing, Sitting sitting) {
        return "\n Appellant: " + hearing.getAppellant()
            + "\nProsecutor: " + hearing.getRespondent()
            + "\nPanel: " + hearing.getJudiciary()
            + "\nTribunal type: " + sitting.getChannel();
    }
}
