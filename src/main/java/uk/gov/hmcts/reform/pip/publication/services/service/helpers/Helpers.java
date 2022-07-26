package uk.gov.hmcts.reform.pip.publication.services.service.helpers;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class Helpers {

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
                return "HHa";
            } else {
                return "HH:mma";
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
        ZonedDateTime zonedDateTime = unZonedDateTime.atZone(zone);
        return zonedDateTime;
    }

    public static String stringDelimiter(String text, String delimiter) {
        if (!text.isEmpty()) {
            return delimiter;
        } else {
            return "";
        }
    }

    public static String findAndReturnNodeText(JsonNode node, String nodeName) {
        if (node.has(nodeName)) {
            return node.get(nodeName).asText();
        } else {
            return "";
        }
    }

    public static String trimAnyCharacterFromStringEnd(String text) {
        text = text.trim();
        if (!text.isEmpty()) {
            return text.replaceAll(",$", "").trim();
        }

        return "";
    }

    public static int convertTimeToMinutes(ZonedDateTime startDateTime,
                                     ZonedDateTime endDateTime) {
        int diffHours = endDateTime.getHour() - startDateTime.getHour();
        int diffMinutes = endDateTime.getMinute() - startDateTime.getMinute();

        return (diffHours * 60)  + diffMinutes;
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
        if (duration > 1) {
            return duration + " " + format + "s";
        } else {
            return duration + " " + format;
        }
    }
}
