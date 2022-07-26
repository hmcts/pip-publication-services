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
/**
 * Service which extracts relevant summary data from each list type to be included in gov.notify emails. For the most
 * part, developing these is a very fiddly process and it doesn't seem like there's much of an easier way. Some
 * refactoring may be helpful as the iteration pattern on JsonNodes is used over and over again.
 */
public class ArtefactSummaryService {

    private static final String COURT_LISTS = "courtLists";
    private static final String COURT_HOUSE = "courtHouse";
    private static final String COURT_ROOM = "courtRoom";
    private static final String SESSION = "session";
    private static final String OFFENCE = "offence";
    private static final String SITTINGS = "sittings";
    private static final String HEARING = "hearing";
    private static final String INDIVIDUAL_DETAILS = "individualDetails";

    /**
     * Parent class to route based on list types.
     *
     * @param payload  - json payload
     * @param listType - list type from artefact
     * @return String which is taken in by the personalisationService to populate bullet points at bottom of
     *     subscriptions email templates.
     * @throws JsonProcessingException - jackson prereq.
     */
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

    /**
     * Civil cause list parent method - iterates on courtHouse/courtList - if these need to be shown in further
     * iterations, do it here.
     *
     * @param payload - json body.
     * @return - string for output.
     * @throws JsonProcessingException - jackson req.
     */
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
        boolean pmdAvoidanceBool = sittingNode.size() > 1;
        while (sittingIterator.hasNext()) {
            outputString.append("\n•Hearing");
            if (pmdAvoidanceBool) {
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

    /**
     * sjp press parent method - iterates over session data. Routes to specific methods which handle offences and
     * judiciary roles.
     *
     * @param payload - json body.
     * @return String with final summary data.
     * @throws JsonProcessingException - jackson req.
     */
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

    /**
     * offences iterator method - handles logic of accused of single or multiple offences and returns output string.
     *
     * @param offencesNode - iterator on offences.
     * @return string with offence data.
     */
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

    /**
     * handles reporting restrictions for sjp press.
     *
     * @param node - node which is checked for reporting restriction.
     * @return - text based on whether restriction exists.
     */
    private String processReportingRestrictionsjpPress(JsonNode node) {
        boolean restriction = node.get("reportingRestriction").asBoolean();
        if (restriction) {
            return "(Reporting restriction)";
        } else {
            return "";
        }
    }

    /**
     * role iteration method for sjp press.
     *
     * @param party - iterator of party.
     * @return list of roles.
     */
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

    /**
     * parent method for sjp public lists. iterates on sittings.
     *
     * @param payload - json body.
     * @return string of data.
     * @throws JsonProcessingException - jackson prereq.
     */
    private String artefactSummarysjpPublic(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder();
        JsonNode node = new ObjectMapper().readTree(payload);
        Iterator<JsonNode> sittings =
            node.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                .get(SESSION).get(0).get(SITTINGS).elements();
        while (sittings.hasNext()) {
            output.append('•');
            JsonNode currentHearing = sittings.next();
            output.append(processRolesSjpPublic(currentHearing));
            String offence = currentHearing.get(HEARING).get(0).get(OFFENCE).get(0).get("offenceTitle").asText();
            output.append("Offence: ").append(offence).append('\n');
        }
        return output.toString();
    }

    /**
     * handle sjp public roles iteration.
     * @param hearing - node of a given hearing.
     * @return string of roles.
     */
    private String processRolesSjpPublic(JsonNode hearing) {
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
