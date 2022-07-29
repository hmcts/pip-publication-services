package uk.gov.hmcts.reform.pip.publication.services.service.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Class for static utility methods assisting with json->html->pdf issues.
 */
public final class Helpers {

    private static final int ONE = 1;

    private Helpers() {
        throw new UnsupportedOperationException();
    }

    public static String formatTimeStampToBst(String timestamp, Boolean isTimeOnly,
                                              Boolean isBothDateAndTime) {
        Instant unZonedDateTime = Instant.parse(timestamp);
        ZoneId zone = ZoneId.of("Europe/London");
        ZonedDateTime zonedDateTime = unZonedDateTime.atZone(zone);
        String pattern = getDateTimeFormat(zonedDateTime, isTimeOnly, isBothDateAndTime);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
        return dtf.format(zonedDateTime);
    }

    private static String getDateTimeFormat(ZonedDateTime zonedDateTime, Boolean isTimeOnly,
                                            Boolean isBothDateAndTime) {
        if (isTimeOnly) {
            if (zonedDateTime.getMinute() == 0) {
                return "ha";
            } else {
                return "h:mma";
            }
        } else if (isBothDateAndTime) {
            return "dd MMMM yyyy HH:mm:ss";
        } else {
            return "dd MMMM yyyy";
        }
    }

    public static ZonedDateTime convertStringToUtc(String timestamp) {
        Instant unZonedDateTime = Instant.parse(timestamp);
        ZoneId zone = ZoneId.of("Europe/London");
        return unZonedDateTime.atZone(zone);
    }

    public static int convertTimeToMinutes(ZonedDateTime startDateTime,
                                           ZonedDateTime endDateTime) {
        int diffHours = endDateTime.getHour() - startDateTime.getHour();
        int diffMinutes = endDateTime.getMinute() - startDateTime.getMinute();

        return diffHours * 60 + diffMinutes;
    }

    public static String formatDuration(int hours, int minutes) {
        if (hours > 0 && minutes > 0) {
            return formatDurationTime(hours, "hour")
                + " " + formatDurationTime(minutes, "min");
        } else if (hours > 0 && minutes == 0) {
            return formatDurationTime(hours, "hour");
        } else if (hours == 0 && minutes > 0) {
            return formatDurationTime(minutes, "min");
        } else {
            return "";
        }
    }

    private static String formatDurationTime(int duration, String format) {
        if (duration > ONE) {
            return duration + " " + format + "s";
        } else {
            return duration + " " + format;
        }
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

    public static String findAndReturnNodeText(JsonNode node, String nodeName) {
        if (node.has(nodeName)) {
            return node.get(nodeName).asText();
        }
        return "";
    }

    public static String stringDelimiter(String text, String delimiter) {
        return text.isEmpty() ? "" : delimiter;
    }

    public static String trimAnyCharacterFromStringEnd(String text) {
        return StringUtils.isBlank(text) ? "" : text.trim().replaceAll(",$", "");
    }

}
