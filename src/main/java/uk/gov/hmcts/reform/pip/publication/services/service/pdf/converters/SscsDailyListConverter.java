package uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SscsDailyList.CourtHouse;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SscsDailyList.CourtRoom;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SscsDailyList.Hearing;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SscsDailyList.Sitting;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.Helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class SscsDailyListConverter implements Converter {

    @Override
    public String convert(JsonNode highestLevelNode, Map<String, String> metadata) throws IOException {
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        Context context = new Context();
        String testString = safeGet("venue.venueContact.venueFax", highestLevelNode);
        context.setVariable("i18n", handleLanguages(metadata));
        context.setVariable("metadata", metadata);
        context.setVariable("telephone", safeGet("venue.venueContact.venueTelephone", highestLevelNode));
        context.setVariable("email", safeGet("venue.venueContact.venueEmail", highestLevelNode));
        context.setVariable("publishedDate", Helpers.formatTimestampToBst(
            safeGet("document.publicationDate", highestLevelNode)));
        List<CourtHouse> listOfCourtHouses = new ArrayList<>();
        for (JsonNode courtHouse : highestLevelNode.get("courtLists")) {
            listOfCourtHouses.add(courtHouseBuilder(courtHouse));
        }
        context.setVariable("courtList", listOfCourtHouses);
        return templateEngine.process("sscsDailyList.html", context);
    }

    private String safeGet(String jsonPath, JsonNode node) {
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
            return outputNode.asText();
        } catch(NullPointerException e){
            log.error("Parsing failed for path " + jsonPath + ", specifically " + stringArray[index]);
            return "";
        }
    }

    private JsonNode safeGetNode(String jsonPath, JsonNode node) {
        String[] stringArray = jsonPath.split("\\.");
        JsonNode outputNode = node;
        int index = 0;
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
        } catch(NullPointerException e){
            log.error("Parsing failed for path " + jsonPath + ", specifically " + stringArray[index]);
            throw new NullPointerException();
        }
    }

    private CourtHouse courtHouseBuilder(JsonNode node) throws JsonProcessingException {
        JsonNode thisCourtHouseNode = node.get("courtHouse");
        CourtHouse thisCourtHouse = new CourtHouse();
        thisCourtHouse.setName(safeGet("courtHouseName", thisCourtHouseNode));
        thisCourtHouse.setPhone(safeGet("courtHouseContact.venueTelephone", thisCourtHouseNode));
        thisCourtHouse.setEmail(safeGet("courtHouseContact.venueEmail", thisCourtHouseNode));
        List<CourtRoom> courtRoomList = new ArrayList<>();
        for (JsonNode courtRoom : thisCourtHouseNode.get("courtRoom")) {
            courtRoomList.add(courtRoomBuilder(courtRoom));
        }
        thisCourtHouse.setListOfCourtRooms(courtRoomList);
        return thisCourtHouse;
    }

    private CourtRoom courtRoomBuilder(JsonNode node) throws JsonProcessingException {
        CourtRoom thisCourtRoom = new CourtRoom();
        thisCourtRoom.setName(safeGet("courtRoomName", node));
        List<Sitting> sittingList = new ArrayList<>();
        for (final JsonNode session : node.get("session")) {
            List<String> sessionChannel = new ObjectMapper().readValue(
                session.get("sessionChannel").toString(),
                new TypeReference<>() {
                }
            );
            String judiciary = formatJudiciary(session);
            String sessionChannelString = String.join(", ", sessionChannel);
            for (JsonNode sitting : session.get("sittings")) {
                sittingList.add(sittingBuilder(sessionChannelString, sitting, judiciary));
            }
        }
        thisCourtRoom.setListOfSittings(sittingList);
        return thisCourtRoom;
    }

    private Sitting sittingBuilder(String sessionChannel, JsonNode node, String judiciary) throws JsonProcessingException {
        Sitting sitting = new Sitting();
        String sittingStart = Helpers.timeStampToBstTime(safeGet("sittingStart", node));
        sitting.setJudiciary(judiciary);
        List<Hearing> listOfHearings = new ArrayList<>();
        if (node.has("channel")) {
            List<String> channelList = new ObjectMapper().readValue(
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

    private Hearing hearingBuilder(JsonNode hearingNode) {
        Hearing currentHearing = new Hearing();
        handleParties(hearingNode.get("party"), currentHearing);
        currentHearing.setRespondent(dealWithInformants(hearingNode));
        currentHearing.setAppealRef(safeGet("case.0.caseNumber", hearingNode));
        return currentHearing;
    }

    private void handleParties(JsonNode node, Hearing hearing) {
        Map<String, String> parties = new HashMap<>();
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
            }
            hearing.setAppellant(parties.get("applicant") + ", Legal Advisor: " + parties.get(
                "applicantRepresentative"));
            hearing.setRespondent(parties.get("respondent") + ", Legal Advisor: " + parties.get(
                "respondentRepresentative"));
        }
    }

    private String dealWithInformants(JsonNode node) {
        List<String> informants = new ArrayList<>();
        safeGetNode("informant.0.prosecutionAuthorityRef", node).forEach(informant -> {
            informants.add(informant.asText());
        });
        return String.join(", ", informants);
    }

    private String individualDetails(JsonNode node) {
        List<String> listOfRetrievedData = new ArrayList<>();
        String[] possibleFields = new String[]{"title", "individualForenames", "individualMiddleName",
            "individualSurname"};
        for (String field : possibleFields) {
            Optional<String> detail = Optional.ofNullable(node.get("individualDetails").findValue(field))
                .map(JsonNode::asText)
                .filter(s -> !s.isEmpty());
            detail.ifPresent(listOfRetrievedData::add);
        }
        return String.join(" ", listOfRetrievedData);
    }


    /**
     * Format the judiciary into a comma seperated string.
     *
     * @param session The session containing the judiciary.
     * @return A string of the formatted judiciary.
     */
    private String formatJudiciary(JsonNode session) {
        StringBuilder formattedJudiciaryBuilder = new StringBuilder();
        session.get("judiciary").forEach(judiciary -> {
            if (formattedJudiciaryBuilder.length() > 0) {
                formattedJudiciaryBuilder.append(", ");
            }
            formattedJudiciaryBuilder
                .append(safeGet("johTitle", judiciary))
                .append(" ").append(safeGet("johNameSurname", judiciary));
        });
        return formattedJudiciaryBuilder.toString();
    }

    private Map<String, Object> handleLanguages(Map<String, String> metadata) throws IOException {
        String path;
        switch (metadata.get("language")) {
            case "ENGLISH": {
                path = "templates/languages/sscs-english.json";
                break;
            }
            case "WELSH":
//                todo: replace with welsh file or refactor to include it as an arg in converter interface.
                path = "templates/languages/sscs-english.json";

                break;
            case "BI_LINGUAL":
//                todo: replace with welsh file or refactor to include it as an arg in converter interface.
                path = "templates/languages/sscs-english.json";
                break;
            default:
                throw new UnsupportedOperationException();
        }
        try (InputStream languageFile = Thread.currentThread()
            .getContextClassLoader().getResourceAsStream(path)) {
            return new ObjectMapper().readValue(
                Objects.requireNonNull(languageFile).readAllBytes(), new TypeReference<>() {
                });
        }
    }


//
//    /**
//     * TODO
//     * @param hearing
//     * @return
//     */
//    private String formatInformants(JsonNode hearing) {
//        StringBuilder formattedProscAuthRefBuilder = new StringBuilder();
//        hearing.get("informant").forEach(informant -> informant.get("prosecutionAuthorityRef").forEach(proscAuthRef -> {
//            if(formattedProscAuthRefBuilder.length() > 0) {
//                formattedProscAuthRefBuilder.append(", ");
//            }
//            formattedProscAuthRefBuilder.append(proscAuthRef);
//        }));
//
//        return formattedProscAuthRefBuilder.toString();
//    }


    // IN THE HTML:
    // Go through court lists
    // Go through court room in court house
    // Go through sessions in court room
    // Go through sittings in session
    // Go through hearing in sitting
    // Go through case in hearing
    // add no border about hearing count


}



