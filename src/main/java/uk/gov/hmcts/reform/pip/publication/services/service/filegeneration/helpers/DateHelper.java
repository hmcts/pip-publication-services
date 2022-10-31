package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

@SuppressWarnings("PMD.TooManyMethods")
public final class DateHelper {

    private static final int ONE = 1;
    public static final String EUROPE_LONDON = "Europe/London";
    private static final int MINUTES_PER_HOUR = 60;
    private static final int HOURS_PER_DAY = 24;

    private DateHelper() {
        throw new UnsupportedOperationException();
    }

    public static String formatTimeStampToBst(String timestamp, Language language, Boolean isTimeOnly,
                                              Boolean isBothDateAndTime) {
        return formatTimeStampToBst(timestamp, language, isTimeOnly, isBothDateAndTime, "dd MMMM yyyy");
    }

    public static String formatTimeStampToBst(String timestamp, Language language, Boolean isTimeOnly,
                                              Boolean isBothDateAndTime, String dateFormat) {
        ZonedDateTime zonedDateTime = convertStringToBst(timestamp);
        String pattern = DateHelper.getDateTimeFormat(zonedDateTime, isTimeOnly, isBothDateAndTime, language,
                                                      dateFormat);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern, Locale.UK);
        return dtf.format(zonedDateTime);
    }

    public static String formatTimeStampToBstHavingWeekDay(String timestamp, String dateFormat,
                                                           Language language) {
        String formattedDate = formatTimeStampToBst(timestamp, language,
            false, false, dateFormat);
        ZonedDateTime zonedDateTime = convertStringToBst(timestamp);
        DayOfWeek dayofWeek = zonedDateTime.getDayOfWeek();
        String day = dayofWeek.getDisplayName(TextStyle.FULL, Locale.UK);
        return day + ' ' + formattedDate;
    }

    private static String getDateTimeFormat(ZonedDateTime zonedDateTime, Boolean isTimeOnly,
                                            Boolean isBothDateAndTime, Language language,
                                            String dateFormat) {
        if (isTimeOnly) {
            if (zonedDateTime.getMinute() == 0) {
                return "ha";
            }
            return "h:mma";
        } else if (isBothDateAndTime) {
            return (language == Language.ENGLISH) ? dateFormat + " 'at' HH:mm" : dateFormat + " 'yn' HH:mm";
        }
        return dateFormat;
    }

    public static String formatLocalDateTimeToBst(LocalDateTime date) {
        return date.format(
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.UK));
    }

    public static long convertTimeToMinutes(ZonedDateTime startDateTime,
                                           ZonedDateTime endDateTime) {
        return Duration.between(startDateTime, endDateTime).toMinutes();
    }

    static String formatDurationInDays(int days, Language language) {
        return (language == Language.ENGLISH)
            ? formatDurationTime(days, "day")
            : formatDurationTime(days, "dydd/day");
    }

    static String formatDuration(int hours, int minutes, Language language) {
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
            ? formatDurationTime(hours, "hour") + " " + formatDurationTime(minutes, "min")
            : formatDurationTime(hours, "awr/hour") + " " + formatDurationTime(minutes, "munud/minute");
    }

    private static String hoursOnly(int hours, Language language) {
        return (language == Language.ENGLISH)
            ? formatDurationTime(hours, "hour")
            : formatDurationTime(hours, "awr/hour");
    }

    private static String minsOnly(int mins, Language language) {
        return (language == Language.ENGLISH)
            ? formatDurationTime(mins, "min")
            : formatDurationTime(mins, "munud/minute");
    }

    private static String formatDurationTime(int duration, String format) {
        return duration > ONE ? duration + " " + format + "s" : duration + " " + format;
    }

    public static ZonedDateTime convertStringToBst(String timestamp) {
        Instant unZonedDateTime = Instant.parse(timestamp);
        ZoneId zone = ZoneId.of(EUROPE_LONDON);
        return unZonedDateTime.atZone(zone);
    }

    public static String timeStampToBstTime(String timestamp) {
        ZonedDateTime zonedDateTime = convertStringToBst(timestamp);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
        return dtf.format(zonedDateTime);
    }


    public static String timeStampToBstTimeWithFormat(String timestamp, String format) {
        ZonedDateTime zonedDateTime = convertStringToBst(timestamp);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format).withLocale(Locale.UK);
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
        calculateDuration(sitting, language, false);
    }

    public static void calculateDuration(JsonNode sitting, Language language, boolean dayCalculation) {
        ZonedDateTime sittingStart = convertStringToBst(sitting.get("sittingStart").asText());
        ZonedDateTime sittingEnd = convertStringToBst(sitting.get("sittingEnd").asText());

        double durationAsHours = 0;
        double durationAsMinutes = convertTimeToMinutes(sittingStart, sittingEnd);

        if (durationAsMinutes >= MINUTES_PER_HOUR) {
            durationAsHours = Math.floor(durationAsMinutes / MINUTES_PER_HOUR);
            durationAsMinutes = durationAsMinutes - (durationAsHours * MINUTES_PER_HOUR);
        }

        String formattedDuration;
        if (dayCalculation && durationAsHours >= HOURS_PER_DAY) {
            formattedDuration = formatDurationInDays((int) Math.floor(durationAsHours / HOURS_PER_DAY), language);
        } else {
            formattedDuration = formatDuration(
                (int) durationAsHours,
                (int) durationAsMinutes, language
            );
        }

        ((ObjectNode) sitting).put("formattedDuration", formattedDuration);
    }

    public static void formatStartTime(JsonNode sitting, String format) {
        ZonedDateTime sittingStart = convertStringToBst(sitting.get("sittingStart").asText());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format).withLocale(Locale.UK);
        String time = dtf.format(sittingStart);

        ((ObjectNode) sitting).put("time", time);
    }
}
