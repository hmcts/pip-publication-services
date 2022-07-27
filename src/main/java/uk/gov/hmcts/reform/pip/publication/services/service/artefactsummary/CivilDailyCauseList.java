package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;

@Service
public class CivilDailyCauseList {

    private static final String COURT_LISTS = "courtLists";
    private static final String COURT_HOUSE = "courtHouse";
    private static final String COURT_ROOM = "courtRoom";
    private static final String SESSION = "session";
    private static final String SITTINGS = "sittings";

    /**
     * Civil cause list parent method - iterates on courtHouse/courtList - if these need to be shown in further
     * iterations, do it here.
     *
     * @param payload - json body.
     * @return - string for output.
     * @throws JsonProcessingException - jackson req.
     */
    public String artefactSummaryCivilDailyCause(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder("");
        JsonNode node = new ObjectMapper().readTree(payload);
        Iterator<JsonNode> courtHouseNode = node.get(COURT_LISTS).elements();
        while (courtHouseNode.hasNext()) {
            JsonNode thisCourtHouse = courtHouseNode.next().get(COURT_HOUSE);
            output.append(processCivilDailyCourtRooms(thisCourtHouse)).append('\n');
        }
        return output.toString();
    }

    /**
     * court room iteration - cycles through courtrooms and deals with routing for hearing channel, judiciary
     * and sitting methods.
     *
     * @param node - jsonnode of courtrooms.
     * @return string with above-mentioned info.
     */
    private String processCivilDailyCourtRooms(JsonNode node) {
        Iterator<JsonNode> courtRoomNode = node.get(COURT_ROOM).elements();
        ObjectMapper mapper = new ObjectMapper();
        StringBuilder outputString = new StringBuilder();
        List<String> sessionChannel;
        TypeReference<List<String>> typeReference = new TypeReference<>() {
        };
        while (courtRoomNode.hasNext()) {
            JsonNode thisCourtRoom = courtRoomNode.next();
            JsonNode sessionChannelNode = thisCourtRoom.get(SESSION).get(0).path("sessionChannel");
            sessionChannel = mapper.convertValue(sessionChannelNode, typeReference);
            outputString.append("\n\nCourtroom: ").append(thisCourtRoom.get("courtRoomName").asText())
                .append(processCivilDailySittings(thisCourtRoom, sessionChannel))
                .append(processCivilDailyJudiciary(thisCourtRoom));
        }
        return outputString.toString();
    }

    /**
     * Judiciary iteration - gets title and known as fields from judiciary node.
     *
     * @param node - node of judiciary.
     * @return judiciary string
     */
    private String processCivilDailyJudiciary(JsonNode node) {
        JsonNode judiciaryNode = node.get(SESSION).get(0).get("judiciary");
        if (judiciaryNode.isEmpty()) {
            return "";
        }
        Iterator<JsonNode> johNode = judiciaryNode.elements();
        StringBuilder johName = new StringBuilder("\nJudiciary: ");
        while (johNode.hasNext()) {
            JsonNode currentJoh = johNode.next();
            String title = currentJoh.path("johTitle").asText();
            String knownAs = currentJoh.path("johKnownAs").asText();
            johName.append(title).append(' ');
            johName.append(knownAs);
            if (johNode.hasNext()) {
                johName.append(", ");
            }
        }
        return johName.toString();
    }

    /**
     * sitting iteration class - deals with hearing channel, start time and hearing data (e.g. case names, refs etc)
     *
     * @param node - node of sittings.
     * @param sessionChannel - session channel passed in from parent method - overridden if sitting level channel
     *                       exists.
     * @return string of these bits.
     */
    private String processCivilDailySittings(JsonNode node, List<String> sessionChannel) {
        JsonNode sittingNode = node.get(SESSION).get(0).get(SITTINGS);
        Iterator<JsonNode> sittingIterator = sittingNode.elements();
        StringBuilder outputString = new StringBuilder(26);
        int counter = 1;
        // below line is due to pmd "avoid using literals in conditional statements" rule.
        boolean sittingNodeSizeBool = sittingNode.size() > 1;
        while (sittingIterator.hasNext()) {
            outputString.append("\n•Hearing");
            if (sittingNodeSizeBool) {
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

    /**
     * hearing channel handler. Sitting channel takes precedence over session channel if both exist (session channel
     * is mandatory, however).
     *
     * @param sessionChannel - mentioned above.
     * @param currentSittingNode - node for getting current sitting channel data.
     * @return - string of correct channel.
     */
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

    /**
     * hearing iterator - gets case names, refs and hearing types.
     *
     * @param node - iterator of hearings.
     * @return String with that stuff in it.
     */
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
}
