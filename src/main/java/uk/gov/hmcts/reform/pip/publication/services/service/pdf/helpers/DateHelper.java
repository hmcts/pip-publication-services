package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@SuppressWarnings("PMD.TooManyMethods")
public final class DateHelper {

    private static final int ONE = 1;
    public static final String EUROPE_LONDON = "Europe/London";
    private static final int MINUTES_PER_HOUR = 60;

    private DateHelper() {
        throw new UnsupportedOperationException();
    }

    public static String formatTimeStampToBst(String timestamp, Language language, Boolean isTimeOnly,
                                              Boolean isBothDateAndTime) {
        ZonedDateTime zonedDateTime = convertStringToBst(timestamp);
        String pattern = DateHelper.getDateTimeFormat(zonedDateTime, isTimeOnly, isBothDateAndTime, language);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern, Locale.UK);
        return dtf.format(zonedDateTime);
    }

    private static String getDateTimeFormat(ZonedDateTime zonedDateTime, Boolean isTimeOnly,
                                            Boolean isBothDateAndTime, Language language) {
        if (isTimeOnly) {
            if (zonedDateTime.getMinute() == 0) {
                return "ha";
            }
            return "h:mma";
        } else if (isBothDateAndTime) {
            return (language == Language.ENGLISH) ? "dd MMMM yyyy 'at' HH:mm" : "dd MMMM yyyy 'yn' HH:mm";
        }
        return "dd MMMM yyyy";
    }

    public static String formatLocalDateTimeToBst(LocalDateTime date) {
        return date.format(
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.UK));
    }

    public static ZonedDateTime convertStringToBst(String timestamp) {
        Instant unZonedDateTime = Instant.parse(timestamp);
        ZoneId zone = ZoneId.of(EUROPE_LONDON);
        return unZonedDateTime.atZone(zone);
    }

    public static int convertTimeToMinutes(ZonedDateTime startDateTime,
                                           ZonedDateTime endDateTime) {
        int diffHours = endDateTime.getHour() - startDateTime.getHour();
        int diffMinutes = endDateTime.getMinute() - startDateTime.getMinute();

        return diffHours * 60 + diffMinutes;
    }

    public static String formatDuration(int hours, int minutes, Language language) {
        if (hours > 0 && minutes > 0) {
            return hoursAndMins(hours, minutes, language);
        } else if (hours > 0 && minutes == 0) {
            return hoursOnly(hours, language);
        } else if (hours == 0 && minutes > 0) {
            return minsOnly(minutes, language);
        }
        return "";
    }

    private static String hoursAndMins(int hours, int minutes, Language language) {
        return (language == Language.ENGLISH)
            ? formatDurationTime(hours, "hour") + " " + formatDurationTime(minutes, "min") :
            formatDurationTime(hours, "awr/hour") + " " + formatDurationTime(minutes, "munud/minute");
    }

    private static String hoursOnly(int hours, Language language) {
        return (language == Language.ENGLISH) ? formatDurationTime(hours, "hour") : formatDurationTime(
            hours, "awr/hour");
    }

    private static String minsOnly(int mins, Language language) {
        return (language == Language.ENGLISH) ? formatDurationTime(mins, "min") : formatDurationTime(
            mins, "munud/minute");
    }

    private static String formatDurationTime(int duration, String format) {
        if (duration > ONE) {
            return duration + " " + format + "s";
        }

        return duration + " " + format;
    }

    public static ZonedDateTime convertStringToUtc(String timestamp) {
        Instant unZonedDateTime = Instant.parse(timestamp);
        ZoneId zone = ZoneId.of(EUROPE_LONDON);
        return unZonedDateTime.atZone(zone);
    }

    public static String timeStampToBstTime(String timestamp) {
        Instant unZonedDateTime = Instant.parse(timestamp);
        ZoneId zone = ZoneId.of(EUROPE_LONDON);
        ZonedDateTime zonedDateTime = unZonedDateTime.atZone(zone);
        DateTimeFormatter dtf;
        dtf = DateTimeFormatter.ofPattern("HH:mm");
        return dtf.format(zonedDateTime);
    }

    public static String formatTimestampToBst(String timestamp, Language language) {
        Instant unZonedDateTime = Instant.parse(timestamp);
        ZoneId zone = ZoneId.of(EUROPE_LONDON);
        ZonedDateTime zonedDateTime = unZonedDateTime.atZone(zone);
        DateTimeFormatter dtf;
        dtf = (language == Language.ENGLISH)
            ? DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm") :
            DateTimeFormatter.ofPattern("dd MMMM yyyy 'yn' HH:mm");
        return dtf.format(zonedDateTime);
    }

    public static void calculateDuration(JsonNode sitting, Language language) {
        ZonedDateTime sittingStart = convertStringToUtc(sitting.get("sittingStart").asText());
        ZonedDateTime sittingEnd = convertStringToUtc(sitting.get("sittingEnd").asText());

        double durationAsHours = 0;
        double durationAsMinutes = convertTimeToMinutes(sittingStart, sittingEnd);

        if (durationAsMinutes >= MINUTES_PER_HOUR) {
            durationAsHours = Math.floor(durationAsMinutes / MINUTES_PER_HOUR);
            durationAsMinutes = durationAsMinutes - (durationAsHours * MINUTES_PER_HOUR);
        }

        String formattedDuration = formatDuration(
            (int) durationAsHours,
            (int) durationAsMinutes, language
        );

        ((ObjectNode) sitting).put("formattedDuration", formattedDuration);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
        String time = dtf.format(sittingStart);

        ((ObjectNode) sitting).put("time", time);
    }
}
