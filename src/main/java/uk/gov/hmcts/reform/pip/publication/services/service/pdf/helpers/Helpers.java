package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

<<<<<<< HEAD
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;

=======
>>>>>>> master
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Class for static utility methods assisting with json->html->pdf issues.
 */
<<<<<<< HEAD
@Slf4j
=======
>>>>>>> master
public final class Helpers {

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
<<<<<<< HEAD

    public static String timeStampToBstTime(String timestamp) {
        Instant unZonedDateTime = Instant.parse(timestamp);
        ZoneId zone = ZoneId.of("Europe/London");
        ZonedDateTime zonedDateTime = unZonedDateTime.atZone(zone);
        DateTimeFormatter dtf;
        dtf = DateTimeFormatter.ofPattern("HH:mm");
        return dtf.format(zonedDateTime);
    }

    public static String safeGet(String jsonPath, JsonNode node) {
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
        } catch (NullPointerException e) {
            log.error("Parsing failed for path " + jsonPath + ", specifically " + stringArray[index]);
            return "";
        }
    }

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
            throw new NullPointerException();
        }
    }
=======
>>>>>>> master
}
