package uk.gov.hmcts.reform.pip.publication.services.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ListType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;


@Service
@Slf4j
public class ArtefactSummaryService {

    private static final String COURT_LISTS = "courtLists";
    private static final String COURT_HOUSE = "courtHouse";
    private static final String COURT_ROOM = "courtRoom";
    private static final String SESSION = "session";
    private static final String SITTINGS = "sittings";
    private static final String HEARING = "hearing";
    private static final String INDIVIDUAL_DETAILS = "individualDetails";

    public String artefactSummary(String payload, ListType listType) throws JsonProcessingException {
        switch (listType) {
            case SJP_PUBLIC_LIST:
                return artefactSummarySJPPublic(payload);
            case SJP_PRESS_LIST:
                return artefactSummarySJPPress(payload);
            case CIVIL_DAILY_CAUSE_LIST:
                return artefactSummaryCivilDailyCause(payload);
            case FAMILY_DAILY_CAUSE_LIST:
                return "fam_cause";
        }
        return "error";
    }

    public String artefactSummaryCivilDailyCause(String payload) throws JsonProcessingException {
//        todo : Case Name, Hearing Type, Time, Case Ref, JOH.
        StringBuilder output = new StringBuilder("");
        JsonNode node = new ObjectMapper().readTree(payload);
        Iterator<JsonNode> courtHouseNode = node.get("courtLists").elements();
        while (courtHouseNode.hasNext()) {
            JsonNode thisCourtHouse = courtHouseNode.next().get("courtHouse");

//            output.append("\n•Courthouse: ")
//                .append(thisCourtHouse.get("courtHouseName").asText());
            output.append(processCivilDailyCourtRooms(thisCourtHouse)).append("\n");
        }
        return output.toString();
    }

    public String processCivilDailyCourtRooms(JsonNode node) {
        Iterator<JsonNode> courtRoomNode = node.get("courtRoom").elements();
        StringBuilder outputString = new StringBuilder();
        while (courtRoomNode.hasNext()) {
            JsonNode thisCourtRoom = courtRoomNode.next();
            JsonNode sessionChannelNode =thisCourtRoom.get("session").get(0).path("sessionChannel");
            ObjectMapper mapper = new ObjectMapper();
            List<String> sessionChannel = mapper.convertValue(sessionChannelNode, new TypeReference<List<String>>() {
            });
            outputString.append("\n\nCourtroom: ").append(thisCourtRoom.get("courtRoomName").asText());
            outputString.append(processCivilDailySittings(thisCourtRoom, sessionChannel));
            outputString.append(processCivilDailyJudiciary(thisCourtRoom));
        }
        return outputString.toString();
    }

    public String processCivilDailyJudiciary(JsonNode node) {
        JsonNode judiciaryNode = node.get("session").get(0).get("judiciary");
        if(judiciaryNode.isEmpty()){
            return "";
        }
        Iterator<JsonNode> johNode = judiciaryNode.elements();
        StringBuilder johName = new StringBuilder("\nJudiciary: ");
        int counter = 1;
        int length = judiciaryNode.size();
        while (johNode.hasNext()) {
            JsonNode currentJoh = johNode.next();
            String title = currentJoh.path("johTitle").asText();
            String knownAs = currentJoh.path("johKnownAs").asText();
            johName.append(title).append(" ");
            johName.append(knownAs);
            if (counter < length) {
                johName.append(", ");
            }
            counter += 1;
        }
        return johName.toString();
    }

    public String processCivilDailySittings(JsonNode node, List<String> sessionChannel) {
        JsonNode sittingNode = node.get("session").get(0).get("sittings");
        Iterator<JsonNode> sittingIterator = sittingNode.elements();
        StringBuilder outputString = new StringBuilder();
        int counter = 1;
        while (sittingIterator.hasNext()) {
            outputString.append("\n•Hearing");
            if (sittingNode.size() > 1) {
                outputString.append(" ").append(counter);
                counter += 1;
            }

            JsonNode currentSitting = sittingIterator.next();
            DateTimeFormatter inputfmt = DateTimeFormatter.ISO_DATE_TIME;
            DateTimeFormatter outputfmt = DateTimeFormatter.ofPattern("hh:mm a");

            String startTime =
                outputfmt.format(LocalDateTime.from(inputfmt.parse(currentSitting.get("sittingStart").asText())));

            outputString.append(processCivilDailyHearings(currentSitting))
                .append(": \nStart Time: ").append(startTime)
                .append(processCivilDailyChannels(sessionChannel, currentSitting.path("channel")));
        }
        return outputString.toString();
    }

    public String processCivilDailyChannels(List<String> sessionChannel, JsonNode currentSittingNode) {
        StringBuilder outputString = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();
        if(currentSittingNode.isMissingNode()){
            for(String channel: sessionChannel){
                outputString.append("\n").append(channel);
            }
        }
        else {
            List<String> channelList = mapper.convertValue(currentSittingNode, new TypeReference<List<String>>() {
            });
            for(String channel: channelList){
                outputString.append("\n").append(channel);
            }
        }
        return outputString.toString();
    }

    public String processCivilDailyHearings(JsonNode node) {
        StringBuilder output = new StringBuilder();
        Iterator<JsonNode> hearingNode = node.get("hearing").elements();
        while (hearingNode.hasNext()) {
            JsonNode currentHearing = hearingNode.next();
            String hearingType = currentHearing.get("hearingType").asText();
            String caseName = currentHearing.get("case").get(0).get("caseName").asText();
            String caseNumber = currentHearing.get("case").get(0).get("caseNumber").asText();
            output.append("\nCase Name: ").append(caseName)
                .append("\nCase Reference: ").append(caseNumber)
                .append("\nHearing Type: ").append(hearingType);
        }
        return output.toString();
    }

    public String artefactSummarySJPPress(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder();
        JsonNode node = new ObjectMapper().readTree(payload);
        Iterator<JsonNode> endNode =
            node.get("courtLists").get(0).get("courtHouse").get("courtRoom").get(0).get("session").get(0).get(
                "sittings").get(0).get("hearing").elements();
        while (endNode.hasNext()) {
            output.append("•");
            JsonNode currentCase = endNode.next();
            output.append(processRolesSJPPress(currentCase));
            output.append(processOffencesSJPPress(currentCase.get("offence"))).append("\n");
        }
        return output.toString();
    }

    public String processOffencesSJPPress(JsonNode offencesNode) {
        StringBuilder outputString = new StringBuilder();
        if (offencesNode.size() > 1) {
            Iterator<JsonNode> offences = offencesNode.elements();
            int counter = 1;
            while (offences.hasNext()) {
                JsonNode thisOffence = offences.next();
                outputString.append("\nOffence ").append(counter).append(": ")
                    .append(thisOffence.get("offenceTitle").asText());
                outputString.append(processReportingRestrictionSJPPress(thisOffence));
                counter += 1;
            }
        } else {
            outputString.append("\nOffence: ").append(offencesNode.get(0).get("offenceTitle").asText())
                .append(processReportingRestrictionSJPPress(offencesNode.get(0)));
        }
        return outputString.toString();
    }

    public String processReportingRestrictionSJPPress(JsonNode node) {
        boolean restriction = node.get("reportingRestriction").asBoolean();
        if (restriction) {
            return "(Reporting restriction)";
        } else {
            return "";
        }
    }

    public String processRolesSJPPress(JsonNode party) {
        Iterator<JsonNode> partyNode = party.get("party").elements();
        String accused = "";
        String postCode = "";
        String prosecutor = "";
        while (partyNode.hasNext()) {
            JsonNode currentParty = partyNode.next();
            if (currentParty.get("partyRole").asText().toLowerCase(Locale.ROOT).equals("accused")) {
                String forename = currentParty.get(INDIVIDUAL_DETAILS).get("individualForenames").asText();
                String surname = currentParty.get(INDIVIDUAL_DETAILS).get("individualSurname").asText();
                postCode = currentParty.get(INDIVIDUAL_DETAILS).get("address").get("postCode").asText();
                accused = forename + " " + surname;
            } else {
                prosecutor = currentParty.get("organisationDetails").get("organisationName").asText();
            }
        }
        return "Accused: " + accused + "\nPostcode: " + postCode + "\nProsecutor: " + prosecutor;
    }

    private String artefactSummarySJPPublic(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder();
        JsonNode node = new ObjectMapper().readTree(payload);
        Iterator<JsonNode> sittings =
            node.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                .get(SESSION).get(0).get(SITTINGS).elements();

        while (sittings.hasNext()) {
            output.append("•");
            JsonNode currentHearing = sittings.next();
            output.append(processRolesSJPPublic(currentHearing));
            String offence = currentHearing.get(HEARING).get(0).get("offence").get(0).get("offenceTitle").asText();
            output.append("Offence: ").append(offence).append("\n");
        }

        return output.toString();
    }

    private String processRolesSJPPublic(JsonNode hearing) {
        StringBuilder outputString = new StringBuilder();
        Iterator<JsonNode> partyNames = hearing.get(HEARING).get(0).get("party").elements();

        while (partyNames.hasNext()) {
            JsonNode currentParty = partyNames.next();
            switch (currentParty.get("partyRole").asText()) {
                case "ACCUSED":
                    String forenames = currentParty.get(INDIVIDUAL_DETAILS).get("individualForenames").asText();
                    String surname = currentParty.get(INDIVIDUAL_DETAILS).get("individualSurname").asText();
                    String postCode = currentParty.get(INDIVIDUAL_DETAILS).get("address").get("postCode").asText();
                    outputString.append("Accused: ").append(forenames).append(" ").append(surname).append("\n");
                    outputString.append("Postcode: ").append(postCode).append("\n");
                    break;
                case "PROSECUTOR":
                    outputString.append("Prosecutor: ").append(currentParty.get("organisationDetails").get(
                        "organisationName").asText()).append("\n");
                    break;
            }
        }
        return outputString.toString();
    }
}
