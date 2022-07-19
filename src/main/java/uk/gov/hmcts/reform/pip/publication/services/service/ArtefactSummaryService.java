package uk.gov.hmcts.reform.pip.publication.services.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ListType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;


@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class ArtefactSummaryService {

    private static final String COURT_LISTS = "courtLists";
    private static final String COURT_HOUSE = "courtHouse";
    private static final String COURT_ROOM = "courtRoom";
    private static final String SESSION = "session";
    private static final String OFFENCE = "offence";
    private static final String SITTINGS = "sittings";
    private static final String HEARING = "hearing";
    private static final String INDIVIDUAL_DETAILS = "individualDetails";

    public String artefactSummary(String payload, ListType listType) throws JsonProcessingException {
        switch (listType) {
            case SJP_PUBLIC_LIST:
                return artefactSummarysjpPublic(payload);
            case SJP_PRESS_LIST:
                return artefactSummarysjpPress(payload);
            case CIVIL_DAILY_CAUSE_LIST:
                return artefactSummaryCivilDailyCause(payload);
            case FAMILY_DAILY_CAUSE_LIST:
                return "fam_cause";
            default:
                return "";
        }
    }

    private String artefactSummaryCivilDailyCause(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder("");
        JsonNode node = new ObjectMapper().readTree(payload);
        Iterator<JsonNode> courtHouseNode = node.get(COURT_LISTS).elements();
        while (courtHouseNode.hasNext()) {
            JsonNode thisCourtHouse = courtHouseNode.next().get(COURT_HOUSE);
            output.append(processCivilDailyCourtRooms(thisCourtHouse)).append('\n');
        }
        return output.toString();
    }

    private String processCivilDailyCourtRooms(JsonNode node) {
        Iterator<JsonNode> courtRoomNode = node.get(COURT_ROOM).elements();
        ObjectMapper mapper = new ObjectMapper();
        StringBuilder outputString = new StringBuilder();
        List<String> sessionChannel;
        TypeReference<List<String>> typeReference = new TypeReference<>(){};
        while (courtRoomNode.hasNext()) {
            JsonNode thisCourtRoom = courtRoomNode.next();
            JsonNode sessionChannelNode = thisCourtRoom.get(SESSION).get(0).path("sessionChannel");
            sessionChannel = mapper.convertValue(sessionChannelNode, typeReference);
            outputString.append("\n\nCourtroom: ").append(thisCourtRoom.get("courtRoomName").asText());
            outputString.append(processCivilDailySittings(thisCourtRoom, sessionChannel));
            outputString.append(processCivilDailyJudiciary(thisCourtRoom));
        }
        return outputString.toString();
    }

    private String processCivilDailyJudiciary(JsonNode node) {
        JsonNode judiciaryNode = node.get(SESSION).get(0).get("judiciary");
        if (judiciaryNode.isEmpty()) {
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
            johName.append(title).append(' ');
            johName.append(knownAs);
            if (counter < length) {
                johName.append(", ");
            }
            counter += 1;
        }
        return johName.toString();
    }

    private String processCivilDailySittings(JsonNode node, List<String> sessionChannel) {
        JsonNode sittingNode = node.get(SESSION).get(0).get(SITTINGS);
        Iterator<JsonNode> sittingIterator = sittingNode.elements();
        StringBuilder outputString = new StringBuilder(26);
        int counter = 1;
        boolean sorryPmd = sittingNode.size() > 1;
        while (sittingIterator.hasNext()) {
            outputString.append("\n•Hearing");
            if (sorryPmd) {
                outputString.append(' ').append(counter);
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

    private String processCivilDailyChannels(List<String> sessionChannel, JsonNode currentSittingNode) {
        StringBuilder outputString = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();
        if (currentSittingNode.isMissingNode()) {
            for (String channel : sessionChannel) {
                outputString.append('\n').append(channel);
            }
        } else {
            List<String> channelList = mapper.convertValue(currentSittingNode, new TypeReference<List<String>>() {
            });
            for (String channel : channelList) {
                outputString.append('\n').append(channel);
            }
        }
        return outputString.toString();
    }

    private String processCivilDailyHearings(JsonNode node) {
        StringBuilder output = new StringBuilder(47);
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

    private String artefactSummarysjpPress(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder();
        JsonNode node = new ObjectMapper().readTree(payload);
        Iterator<JsonNode> endNode =
            node.get("courtLists").get(0).get("courtHouse").get("courtRoom").get(0).get(SESSION).get(0).get(
                "sittings").get(0).get("hearing").elements();
        while (endNode.hasNext()) {
            output.append('•');
            JsonNode currentCase = endNode.next();
            output.append(processRolessjpPress(currentCase));
            output.append(processOffencessjpPress(currentCase.get("offence"))).append('\n');
        }
        return output.toString();
    }

    private String processOffencessjpPress(JsonNode offencesNode) {
        StringBuilder outputString = new StringBuilder();
        boolean sorryPmd = offencesNode.size() > 1;
        if (sorryPmd) {
            Iterator<JsonNode> offences = offencesNode.elements();
            int counter = 1;
            while (offences.hasNext()) {
                JsonNode thisOffence = offences.next();
                outputString.append("\nOffence ").append(counter).append(": ")
                    .append(thisOffence.get("offenceTitle").asText());
                outputString.append(processReportingRestrictionsjpPress(thisOffence));
                counter += 1;
            }
        } else {
            outputString.append("\nOffence: ").append(offencesNode.get(0).get("offenceTitle").asText())
                .append(processReportingRestrictionsjpPress(offencesNode.get(0)));
        }
        return outputString.toString();
    }

    private String processReportingRestrictionsjpPress(JsonNode node) {
        boolean restriction = node.get("reportingRestriction").asBoolean();
        if (restriction) {
            return "(Reporting restriction)";
        } else {
            return "";
        }
    }

    private String processRolessjpPress(JsonNode party) {
        Iterator<JsonNode> partyNode = party.get("party").elements();
        String accused = "";
        String postCode = "";
        String prosecutor = "";
        while (partyNode.hasNext()) {
            JsonNode currentParty = partyNode.next();
            if ("accused".equals(currentParty.get("partyRole").asText().toLowerCase(Locale.ROOT))) {
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

    private String artefactSummarysjpPublic(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder();
        JsonNode node = new ObjectMapper().readTree(payload);
        Iterator<JsonNode> sittings =
            node.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                .get(SESSION).get(0).get(SITTINGS).elements();

        while (sittings.hasNext()) {
            output.append('•');
            JsonNode currentHearing = sittings.next();
            output.append(processRolessjpPublic(currentHearing));
            String offence = currentHearing.get(HEARING).get(0).get(OFFENCE).get(0).get("offenceTitle").asText();
            output.append("Offence: ").append(offence).append('\n');
        }

        return output.toString();
    }

    private String processRolessjpPublic(JsonNode hearing) {
        StringBuilder outputString = new StringBuilder();
        Iterator<JsonNode> partyNames = hearing.get(HEARING).get(0).get("party").elements();

        while (partyNames.hasNext()) {
            JsonNode currentParty = partyNames.next();
            switch (currentParty.get("partyRole").asText()) {
                case "ACCUSED":
                    String forenames = currentParty.get(INDIVIDUAL_DETAILS).get("individualForenames").asText();
                    String surname = currentParty.get(INDIVIDUAL_DETAILS).get("individualSurname").asText();
                    String postCode = currentParty.get(INDIVIDUAL_DETAILS).get("address").get("postCode").asText();
                    outputString.append("Accused: ").append(forenames).append(' ').append(surname);
                    outputString.append("\nPostcode: ").append(postCode).append('\n');
                    break;
                case "PROSECUTOR":
                    outputString.append("Prosecutor: ")
                        .append(currentParty.get("organisationDetails").get("organisationName").asText())
                        .append('\n');
                    break;
                default:
                    break;
            }
        }
        return outputString.toString();
    }
}
