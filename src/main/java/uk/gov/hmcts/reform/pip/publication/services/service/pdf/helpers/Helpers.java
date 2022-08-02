package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.sscsdailylist.CourtHouse;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.sscsdailylist.CourtRoom;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.sscsdailylist.Hearing;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.sscsdailylist.Sitting;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class for static utility methods assisting with json->html->pdf issues.
 */
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public final class Helpers {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Helpers() {
        throw new UnsupportedOperationException();
    }

    public static String formatTimestampToBst(String timestamp) {
        Instant unZonedDateTime = Instant.parse(timestamp);
        ZoneId zone = ZoneId.of("Europe/London");
        ZonedDateTime zonedDateTime = unZonedDateTime.atZone(zone);
        DateTimeFormatter dtf;
        dtf = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm");
        return dtf.format(zonedDateTime);
    }

    public static String formatLocalDateTimeToBst(LocalDateTime date) {
        return date.format(
            DateTimeFormatter.ofPattern("dd MMMM yyyy"));
    }

    public static String timeStampToBstTime(String timestamp) {
        Instant unZonedDateTime = Instant.parse(timestamp);
        ZoneId zone = ZoneId.of("Europe/London");
        ZonedDateTime zonedDateTime = unZonedDateTime.atZone(zone);
        DateTimeFormatter dtf;
        dtf = DateTimeFormatter.ofPattern("HH:mm");
        return dtf.format(zonedDateTime);
    }

    @SuppressWarnings("PMD.AvoidCatchingNPE")
    public static String safeGet(String jsonPath, JsonNode node) {
        return safeGetNode(jsonPath, node).asText();
    }

    private static void handlePartiesScss(JsonNode node, Hearing hearing) {
        Map<String, String> parties = new ConcurrentHashMap<>();
        for (JsonNode party : node) {
            switch (party.get("partyRole").asText()) {
                case "APPLICANT_PETITIONER":
                    parties.put("applicant", individualDetails(party));
                    break;
                case "APPLICANT_PETITIONER_REPRESENTATIVE":
                    parties.put("applicantRepresentative", individualDetails(party));
                    break;
                case "RESPONDENT":
                    parties.put("respondent", individualDetails(party));
                    break;
                case "RESPONDENT_REPRESENTATIVE":
                    parties.put("respondentRepresentative", individualDetails(party));
                    break;
                default:
                    break;
            }
            hearing.setAppellant(parties.get("applicant") + ",\nLegal Advisor: " + parties.get(
                "applicantRepresentative"));
            hearing.setRespondent(parties.get("respondent") + ",\nLegal Advisor: " + parties.get(
                "respondentRepresentative"));
        }
    }

    private static Hearing hearingBuilder(JsonNode hearingNode) {
        Hearing currentHearing = new Hearing();
        handlePartiesScss(hearingNode.get("party"), currentHearing);
        currentHearing.setRespondent(dealWithInformants(hearingNode));
        currentHearing.setAppealRef(safeGet("case.0.caseNumber", hearingNode));
        return currentHearing;
    }


    private static String dealWithInformants(JsonNode node) {
        List<String> informants = new ArrayList<>();
        safeGetNode("informant.0.prosecutionAuthorityRef", node).forEach(informant -> {
            informants.add(informant.asText());
        });
        return String.join(", ", informants);
    }


    private static String individualDetails(JsonNode node) {
        List<String> listOfRetrievedData = new ArrayList<>();
        String[] possibleFields = {"title", "individualForenames", "individualMiddleName", "individualSurname"};
        for (String field : possibleFields) {
            Optional<String> detail = Optional.ofNullable(node.get("individualDetails").findValue(field))
                .map(JsonNode::asText)
                .filter(s -> !s.isEmpty());
            detail.ifPresent(listOfRetrievedData::add);
        }
        return String.join(" ", listOfRetrievedData);
    }

    @SuppressWarnings("PMD.AvoidCatchingNPE")
    public static JsonNode safeGetNode(String jsonPath, JsonNode node) {
        String[] stringArray = jsonPath.split("\\.");
        JsonNode outputNode = node;
        int index = -1;
        try {
            for (String arg : stringArray) {
                if (NumberUtils.isCreatable(arg)) {
                    outputNode = outputNode.get(Integer.parseInt(arg));
                } else {
                    outputNode = outputNode.get(arg);
                }
                index += 1;
            }
            return outputNode;
        } catch (NullPointerException e) {
            log.error("Parsing failed for path " + jsonPath + ", specifically " + stringArray[index]);
            return node;
        }
    }

    private static Sitting sscsSittingBuilder(String sessionChannel, JsonNode node, String judiciary)
        throws JsonProcessingException {
        Sitting sitting = new Sitting();
        String sittingStart = Helpers.timeStampToBstTime(safeGet("sittingStart", node));
        sitting.setJudiciary(judiciary);
        List<Hearing> listOfHearings = new ArrayList<>();
        if (node.has("channel")) {
            List<String> channelList = MAPPER.readValue(
                node.get("channel").toString(), new TypeReference<>() {
                });
            sitting.setChannel(String.join(", ", channelList));
        } else {
            sitting.setChannel(sessionChannel);
        }
        Iterator<JsonNode> nodeIterator = node.get("hearing").elements();
        while (nodeIterator.hasNext()) {
            JsonNode currentHearingNode = nodeIterator.next();
            Hearing currentHearing = hearingBuilder(currentHearingNode);
            currentHearing.setHearingTime(sittingStart);
            listOfHearings.add(currentHearing);
            currentHearing.setJudiciary(sitting.getJudiciary());
        }
        sitting.setListOfHearings(listOfHearings);
        return sitting;
    }

    /**
     * Format the judiciary into a comma seperated string.
     *
     * @param session The session containing the judiciary.
     * @return A string of the formatted judiciary.
     */
    private static String scssFormatJudiciary(JsonNode session) {
        StringBuilder formattedJudiciaryBuilder = new StringBuilder();
        session.get("judiciary").forEach(judiciary -> {
            if (formattedJudiciaryBuilder.length() > 0) {
                formattedJudiciaryBuilder.append(", ");
            }
            formattedJudiciaryBuilder
                .append(safeGet("johTitle", judiciary))
                .append(' ').append(safeGet("johNameSurname", judiciary));
        });
        return formattedJudiciaryBuilder.toString();
    }

    private static CourtRoom scssCourtRoomBuilder(JsonNode node) throws JsonProcessingException {
        CourtRoom thisCourtRoom = new CourtRoom();
        thisCourtRoom.setName(safeGet("courtRoomName", node));
        List<Sitting> sittingList = new ArrayList<>();
        List<String> sessionChannel;
        TypeReference<List<String>> typeReference = new TypeReference<>() {};
        for (final JsonNode session : node.get("session")) {
            sessionChannel = MAPPER.readValue(
                session.get("sessionChannel").toString(),
                typeReference
            );
            String judiciary = scssFormatJudiciary(session);
            String sessionChannelString = String.join(", ", sessionChannel);
            for (JsonNode sitting : session.get("sittings")) {
                sittingList.add(sscsSittingBuilder(sessionChannelString, sitting, judiciary));
            }
        }
        thisCourtRoom.setListOfSittings(sittingList);
        return thisCourtRoom;
    }

    public static CourtHouse courtHouseBuilder(JsonNode node) throws JsonProcessingException {
        JsonNode thisCourtHouseNode = node.get("courtHouse");
        CourtHouse thisCourtHouse = new CourtHouse();
        thisCourtHouse.setName(safeGet("courtHouseName", thisCourtHouseNode));
        thisCourtHouse.setPhone(safeGet("courtHouseContact.venueTelephone", thisCourtHouseNode));
        thisCourtHouse.setEmail(safeGet("courtHouseContact.venueEmail", thisCourtHouseNode));
        List<CourtRoom> courtRoomList = new ArrayList<>();
        for (JsonNode courtRoom : thisCourtHouseNode.get("courtRoom")) {
            courtRoomList.add(scssCourtRoomBuilder(courtRoom));
        }
        thisCourtHouse.setListOfCourtRooms(courtRoomList);
        return thisCourtHouse;
    }
}
