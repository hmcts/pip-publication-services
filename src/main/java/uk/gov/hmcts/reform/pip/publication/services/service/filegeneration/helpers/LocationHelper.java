package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class LocationHelper {

    private static final String POSTCODE = "postCode";
    static final String COURT_HOUSE = "courtHouse";

    private LocationHelper() {
        throw new UnsupportedOperationException();
    }



    public static List<String> formatVenueAddress(JsonNode artefact) {
        List<String> address = new ArrayList<>();
        JsonNode arrayNode = artefact.get("venue").get("venueAddress").get("line");
        for (JsonNode jsonNode : arrayNode) {
            if (!jsonNode.asText().isEmpty()) {
                address.add(jsonNode.asText());
            }
        }
        if (!GeneralHelper.findAndReturnNodeText(artefact.get("venue").get("venueAddress"), POSTCODE).isEmpty()) {
            address.add(artefact.get("venue").get("venueAddress").get(POSTCODE).asText());
        }
        return address;

    }

    public static void formatCourtAddress(JsonNode artefact) {
        artefact.get("courtLists").forEach(courtList -> {
            StringBuilder formattedCourtAddress = new StringBuilder();

            if (courtList.get(COURT_HOUSE).has("courtHouseAddress")) {
                JsonNode courtHouseAddress = courtList.get(COURT_HOUSE).get("courtHouseAddress");

                GeneralHelper.loopAndFormatString(courtHouseAddress, "line",
                                            formattedCourtAddress, "|");

                checkAndFormatAddress(courtHouseAddress, "town",
                                           formattedCourtAddress, '|');

                checkAndFormatAddress(courtHouseAddress, "county",
                                           formattedCourtAddress, '|');

                checkAndFormatAddress(courtHouseAddress, POSTCODE,
                                           formattedCourtAddress, '|');
            }

            ((ObjectNode)courtList.get(COURT_HOUSE)).put("formattedCourtHouseAddress",
                formattedCourtAddress.toString().replaceAll(", $", ""));
        });
    }

    private static void checkAndFormatAddress(JsonNode node, String nodeName,
                                              StringBuilder builder, Character delimiter) {
        if (!GeneralHelper.findAndReturnNodeText(node, nodeName).isEmpty()) {
            builder
                .append(node.get(nodeName).asText())
                .append(delimiter);
        }
    }

    public static void formatRegionName(JsonNode artefact) {
        try {
            ((ObjectNode) artefact).put("regionName",
                                        artefact.get("locationDetails").get("region").get("name").asText());
        } catch (Exception e) {
            ((ObjectNode) artefact).put("regionName", "");
        }
    }

    public static void formatRegionalJoh(JsonNode artefact) {
        StringBuilder formattedJoh = new StringBuilder();
        try {
            artefact.get("locationDetails").get("region").get("regionalJOH").forEach(joh -> {
                if (formattedJoh.length() != 0) {
                    formattedJoh.append(", ");
                }

                formattedJoh.append(GeneralHelper.findAndReturnNodeText(joh, "johKnownAs"));
                formattedJoh.append(' ');
                formattedJoh.append(GeneralHelper.findAndReturnNodeText(joh, "johNameSurname"));
            });

            ((ObjectNode) artefact).put("regionalJoh", formattedJoh.toString());
        } catch (Exception e) {
            ((ObjectNode) artefact).put("regionalJoh", "");
        }
    }

    static void formattedCourtRoomName(JsonNode courtRoom, JsonNode session,
                                       StringBuilder formattedJudiciary) {
        if (StringUtils.isBlank(formattedJudiciary.toString())) {
            formattedJudiciary.append(courtRoom.get("courtRoomName").asText());
        } else {
            formattedJudiciary.insert(0, courtRoom.get("courtRoomName").asText() + ": ");
        }

        ((ObjectNode)session).put("formattedSessionCourtRoom",
            GeneralHelper.trimAnyCharacterFromStringEnd(formattedJudiciary.toString()));
    }
}
