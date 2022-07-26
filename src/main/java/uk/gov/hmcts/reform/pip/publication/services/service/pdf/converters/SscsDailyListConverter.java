package uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class SscsDailyListConverter implements Converter {

    @Override
    public String convert(JsonNode artefact,  Map<String, String> metadata) {
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        Context context = new Context();
        JsonNode testRemoveMe = formatListData(artefact);
        System.out.println(testRemoveMe);
        context.setVariable("jsonBody", testRemoveMe);
        context.setVariable("location", metadata.get("location"));
        context.setVariable("provenance", metadata.get("provenance"));

        context.setVariable("publicationDate", formatTimeStampToBst(artefact.get("document")
                                                                             .get("publicationDate").asText(), false));
        context.setVariable("publicationTime", formatTimeStampToBst(artefact.get("document")
                                                                             .get("publicationDate").asText(), true));
        context.setVariable("contentDate", formatTimeStampToBst(metadata.get("contentDate"), false));

        return templateEngine.process("sscsDailyList.html", context);
    }


    private JsonNode formatListData(JsonNode listData) {
        listData.get("courtLists").forEach(courtList ->
            courtList.get("courtHouse").get("courtRoom").forEach(
                courtRoom -> courtRoom.get("session").forEach(session -> {
                    ((ObjectNode)session).put("formattedJudiciary", formatJudiciary(session));
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
            if(formattedJudiciaryBuilder.length() > 0) {
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


    private String formatTimeStampToBst(String timestamp, Boolean isTimeOnly){
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



