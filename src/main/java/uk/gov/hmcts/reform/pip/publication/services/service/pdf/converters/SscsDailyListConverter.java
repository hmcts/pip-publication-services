package uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import springfox.documentation.spring.web.json.Json;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SscsDailyList.CourtHouse;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SscsDailyList.CourtRoom;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SscsDailyList.Sitting;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.Helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SscsDailyListConverter implements Converter {

    @Override
    public String convert(JsonNode highestLevelNode, Map<String, String> metadata) throws IOException {
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        Context context = new Context();
        context.setVariable("i18n", handleLanguages(metadata));
        context.setVariable("metadata", metadata);
        context.setVariable("venue", getVenueInfo(highestLevelNode));
        context.setVariable("publishedDate", Helpers.formatTimestampToBst(highestLevelNode.get("document")
                                                                              .get("publicationDate").asText()));
        List<CourtHouse> listOfCourtHouses = new ArrayList<>();
        for (JsonNode courtHouse : highestLevelNode.get("courtLists")) {
            listOfCourtHouses.add(courtHouseBuilder(courtHouse));
        }
        context.setVariable("courtList", listOfCourtHouses);


//        todo get rid of this nonsense:
        JsonNode testRemoveMe = formatListData(highestLevelNode);
        context.setVariable("jsonBody", testRemoveMe);


        return templateEngine.process("sscsDailyList.html", context);
    }


    private CourtHouse courtHouseBuilder(JsonNode node) throws JsonProcessingException {
        JsonNode thisCourtHouseNode = node.get("courtHouse");
        CourtHouse thisCourtHouse = new CourtHouse();
        Map<String, String> metadata = courtHouseInfoRetriever(thisCourtHouseNode);
        thisCourtHouse.setName(metadata.get("name"));
        thisCourtHouse.setPhone(metadata.get("phone"));
        thisCourtHouse.setEmail(metadata.get("email"));
        List<CourtRoom> courtRoomList = new ArrayList<>();
        for(JsonNode courtRoom:  thisCourtHouseNode.get("courtRoom")) {
            courtRoomList.add(courtRoomBuilder(courtRoom));
        }
        thisCourtHouse.setListOfCourtRooms(courtRoomList);
        return thisCourtHouse;
    }

    private Map<String, String> courtHouseInfoRetriever(JsonNode node) {
        return Map.of("name", node.get("courtHouseName").asText(),
                      "phone", node.get("courtHouseContact").get("venueTelephone").asText(),
                      "email", node.get("courtHouseContact").get("venueEmail").asText()
        );
    }

    private CourtRoom courtRoomBuilder(JsonNode node) throws JsonProcessingException {
        CourtRoom thisCourtRoom = new CourtRoom();
        thisCourtRoom.setName(node.get("courtRoomName").asText());
        List<Sitting> sittingList = new ArrayList<>();
        for (final JsonNode session: node.get("session")) {
            JsonNode sittingsNode = session.get("sessionChannel");
            String sittingsString = sittingsNode.toString();
            List<String> seshChannel = new ObjectMapper().readValue(
                session.get("sessionChannel").toString(),
                new TypeReference<List<String>>(){});
            String sessionChannel = String.join(", ", seshChannel);
            for(JsonNode sitting: session.get("sittings")) {
                sittingList.add(sittingBuilder(sessionChannel, sitting));
            }
        }
        thisCourtRoom.setListOfSittings(sittingList);
        return thisCourtRoom;
    }

    private Sitting sittingBuilder(String sessionChannel, JsonNode node) throws JsonProcessingException {
        Sitting sitting = new Sitting();
        sitting.setSittingStart(Helpers.timeStampToBstTime(node.get("sittingStart").asText()));
        if (node.has("channel")) {
            List<String> channelList = new ObjectMapper().readValue(node.get("channel").toString(),
                                                            new TypeReference<List<String>>(){});
            sitting.setChannel(String.join(", ", channelList));
        }
        else {
            sitting.setChannel(sessionChannel);
        }
        return sitting;
    }



    private Map<String, String> getVenueInfo(JsonNode node) {
        return Map.of(
            "telephone", node.get("venue").get("venueContact").get("venueTelephone").asText(),
            "email", node.get("venue").get("venueContact").get("venueEmail").asText()
        );
    }

    private JsonNode formatListData(JsonNode listData) {
        listData.get("courtLists").forEach(courtList ->
                                               courtList.get("courtHouse").get("courtRoom").forEach(
                                                   courtRoom -> courtRoom.get("session").forEach(session -> {
                                                       ((ObjectNode) session).put(
                                                           "formattedJudiciary",
                                                           formatJudiciary(session)
                                                       );
                                                       ((ObjectNode) session).remove("judiciary");
//                    JsonNode formattedSittings = formatSittings(session.get("sittings"));
//                    ((ObjectNode)session).remove("sittings");
//                    ((ObjectNode)session).set("sittings", formattedSittings);
                                                   })));

        return listData;
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
                .append(judiciary.get("johTitle").asText())
                .append(" ")
                .append(judiciary.get("johNameSurname").asText());
        });
        return formattedJudiciaryBuilder.toString();
    }

    private Map<String, String> handleLanguages(Map<String, String> metadata) throws IOException {
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
                languageFile.readAllBytes(), new TypeReference<Map<String, String>>() {
                });
        }
    }

//    private JsonNode formatSittings(JsonNode sittings) {
//        sittings.forEach(sitting -> {
//            // Update sitting start time
//
//            // Set the hearing platform
//            sitting.get("hearing").forEach(hearing -> {
//                ((ObjectNode)d).put("formattedJudiciary", formatJudiciary(session));
//                ((ObjectNode) session).remove("judiciary");
//
//                formatInformants(hearing);
//            });
//        });
//        // Process the sitting then return the JsonNode to update the Sitting with the sitting???
//    }


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



