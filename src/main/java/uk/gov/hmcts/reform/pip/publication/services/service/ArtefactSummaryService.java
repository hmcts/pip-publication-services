package uk.gov.hmcts.reform.pip.publication.services.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ListType;

import java.util.Iterator;
import java.util.Locale;


@Service
@Slf4j
public class ArtefactSummaryService {

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
        while (courtHouseNode.hasNext()){
            JsonNode thisCourtHouse = courtHouseNode.next().get("courtHouse");
            output.append("Courthouse: ")
                .append(thisCourtHouse.get("courtHouseName").asText()).append("\n");
            output.append(processCivilDailyCourtRooms(thisCourtHouse));
        }
        return output.toString();
    }

    public String processCivilDailyCourtRooms(JsonNode node) {
        Iterator<JsonNode> courtRoomNode = node.get("courtRoom").elements();
        StringBuilder outputString = new StringBuilder();
        while (courtRoomNode.hasNext()){
            JsonNode thisCourtRoom = courtRoomNode.next();
            outputString.append("Courtroom: ").append(thisCourtRoom.get("courtRoomName").asText());
//            outputString.append(thisCourtRoom.get(0).get("session").get(0).get("sittings").get(0));
            outputString.append(processCivilDailyJudiciary(thisCourtRoom));
        }
        return outputString.toString();
    }


    public String processCivilDailyJudiciary(JsonNode node) {
        Iterator<JsonNode> johNode = node.get("session").get(0).get("judiciary").elements();
        StringBuilder johName = new StringBuilder();
        while (johNode.hasNext()){
            JsonNode currentJoh = johNode.next();
            String title = currentJoh.get("johTitle").asText();
            String knownAs = currentJoh.get("johKnownAs").asText();
//
//            johName.append(currentJoh.get("johTitle").asText()).append(" ");
//                johName.append(currentJoh.get("johKnownAs").asText());
        }
        return johName.toString();
    }


//    public String processCourtRooms

    public String artefactSummarySJPPress(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder();
        JsonNode node = new ObjectMapper().readTree(payload);
        Iterator<JsonNode> endNode =
            node.get("courtLists").get(0).get("courtHouse").get("courtRoom").get(0).get("session").get(0).get(
                "sittings").get(0).get("hearing").elements();
        while(endNode.hasNext()){
            output.append("•");
            JsonNode currentCase = endNode.next();
            output.append(processRolesSJPPress(currentCase));
            output.append(processOffencesSJPPress(currentCase.get("offence"))).append("\n");
        }
        return output.toString();
    }

    public String processOffencesSJPPress(JsonNode offencesNode) {
       StringBuilder outputString = new StringBuilder();
       if (offencesNode.size() > 1){
           Iterator<JsonNode> offences = offencesNode.elements();
           int counter = 1;
           while(offences.hasNext()){
               JsonNode thisOffence = offences.next();
               outputString.append("\nOffence ").append(counter).append(": ")
                   .append(thisOffence.get("offenceTitle").asText());
               outputString.append(processReportingRestrictionSJPPress(thisOffence));
               counter += 1;
           }
       }
       else {
           outputString.append("\nOffence: ").append(offencesNode.get(0).get("offenceTitle").asText())
           .append(processReportingRestrictionSJPPress(offencesNode.get(0)));
       }
       return outputString.toString();
    }

    public String processReportingRestrictionSJPPress(JsonNode node){
        boolean restriction = node.get("reportingRestriction").asBoolean();
        if (restriction){
            return "(Reporting restriction)";
        }
        else {
            return "";
        }
    }

    public String processRolesSJPPress(JsonNode party) {
        Iterator<JsonNode> partyNode = party.get("party").elements();
        String accused = "";
        String postCode = "";
        String prosecutor = "";
        while (partyNode.hasNext()){
            JsonNode currentParty = partyNode.next();
            if (currentParty.get("partyRole").asText().toLowerCase(Locale.ROOT).equals("accused")){
                String forename = currentParty.get("individualDetails").get("individualForenames").asText();
                String surname = currentParty.get("individualDetails").get("individualSurname").asText();
                postCode = currentParty.get("individualDetails").get("address").get("postCode").asText();
                accused = forename + " " + surname;
            }
            else {
                prosecutor = currentParty.get("organisationDetails").get("organisationName").asText();
            }
        }
        return "Accused: " + accused + "\nPostcode: " + postCode + "\nProsecutor: " + prosecutor;
    }

    public String artefactSummarySJPPublic(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder();
        JsonNode node = new ObjectMapper().readTree(payload);
        Iterator<JsonNode> endNode =
            node.get("courtLists").get(0).get("courtHouse").get("courtRoom").get(0).get("session").get(0).get(
                "sittings").elements();
        while (endNode.hasNext()) {
            output.append("•");
            JsonNode currentHearing = endNode.next();
            output.append(processRolesSJPPublic(currentHearing));
            String offence = currentHearing.get("hearing").get(0).get("offence").get(0).get("offenceTitle").asText();
            output.append("Offence: ").append(offence).append("\n");
        }
        return output.toString();
    }

    public String processRolesSJPPublic(JsonNode hearing) {
        StringBuilder outputString = new StringBuilder();
        Iterator<JsonNode> partyNames = hearing.get("hearing").get(0).get("party").elements();
        while (partyNames.hasNext()) {
            JsonNode currentParty = partyNames.next();
            switch (currentParty.get("partyRole").asText()) {
                case "ACCUSED":
                    String forenames = currentParty.get("individualDetails").get("individualForenames").asText();
                    String surname = currentParty.get("individualDetails").get("individualSurname").asText();
                    String postCode = currentParty.get("individualDetails").get("address").get("postCode").asText();
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
