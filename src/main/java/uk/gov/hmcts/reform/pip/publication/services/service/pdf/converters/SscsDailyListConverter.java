package uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.Helpers;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class SscsDailyListConverter implements Converter {

    @Override
    public String convert(JsonNode highestLevelNode, Map<String, String> metadata) throws IOException {
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        Context context = new Context();
        Map<String, String> i18n = handleLanguages(metadata);
        context.setVariable("i18n", i18n);
        JsonNode testRemoveMe = formatListData(highestLevelNode);
        context.setVariable("jsonBody", testRemoveMe);
        context.setVariable("location", metadata.get("location"));
        context.setVariable("provenance", metadata.get("provenance"));
        context.setVariable("venue", getVenueInfo(highestLevelNode));

        context.setVariable("publishedDate", Helpers.formatTimestampToBst(highestLevelNode.get("document")
                                                                              .get("publicationDate").asText()));
        context.setVariable("publicationTime", formatTimeStampToBst(highestLevelNode.get("document")
                                                                        .get("publicationDate").asText(), true));
        context.setVariable("contentDate", metadata.get("contentDate"));
        context.setVariable(
            "telephone",
            highestLevelNode.get("venue").get("venueContact").get("venueTelephone").asText()
        );

        return templateEngine.process("sscsDailyList.html", context);
    }

    private Map<String, String> handleLanguages(Map<String, String> metadata) throws IOException {
        String path;
        switch (metadata.get("language")) {
            case "ENGLISH": {
                path = "templates/languages/sscs-english.json";
                break;
            }
            case "WELSH":
//                todo: replace with welsh file.
                path = "templates/languages/sscs-english.json";

                break;
            case "BI_LINGUAL":
//                todo: replace with bilingual file
                path = "templates/languages/sscs-english.json";
                break;
            default:
                throw new UnsupportedOperationException();
        }
        try (InputStream languageFile = Thread.currentThread()
            .getContextClassLoader().getResourceAsStream(path)) {
            return new ObjectMapper().readValue(
                languageFile.readAllBytes(), new TypeReference<Map<String, String>>() {});
        }
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
//        // Process the sitting then return the JsonNode to update the Session with the sitting???
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


    private String formatTimeStampToBst(String timestamp, Boolean isTimeOnly) {
        Instant unZonedDateTime = Instant.parse(timestamp);
        ZoneId zone = ZoneId.of("Europe/London");
        ZonedDateTime zonedDateTime = unZonedDateTime.atZone(zone);
        String pattern = this.getDateTimeFormat(zonedDateTime, isTimeOnly);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
        return dtf.format(zonedDateTime);
    }

    private String getDateTimeFormat(ZonedDateTime zonedDateTime, Boolean isTimeOnly) {
        if (isTimeOnly) {
            if (zonedDateTime.getMinute() == 0) {
                return "HHa";
            } else {
                return "HH:mma";
            }
        } else {
            return "dd MMMM yyyy";
        }
    }
}



