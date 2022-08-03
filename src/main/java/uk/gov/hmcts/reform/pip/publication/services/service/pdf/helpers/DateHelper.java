package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateHelper {

    private static final int ONE = 1;

    private DateHelper() {
        throw new UnsupportedOperationException();
    }

    public static String formatTimestampToBstForSjp(String timestamp) {
        Instant unZonedDateTime = Instant.parse(timestamp);
        ZoneId zone = ZoneId.of("Europe/London");
        ZonedDateTime zonedDateTime = unZonedDateTime.atZone(zone);
        DateTimeFormatter dtf;
        dtf = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm");
        return dtf.format(zonedDateTime);
    }

    public static String formatTimeStampToBst(String timestamp, Boolean isTimeOnly,
                                              Boolean isBothDateAndTime) {
        ZonedDateTime zonedDateTime = convertStringToBst(timestamp);
        String pattern = DateHelper.getDateTimeFormat(zonedDateTime, isTimeOnly, isBothDateAndTime);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern, Locale.UK);
        return dtf.format(zonedDateTime);
    }

    private static String getDateTimeFormat(ZonedDateTime zonedDateTime, Boolean isTimeOnly,
                                            Boolean isBothDateAndTime) {
        if (isTimeOnly) {
            if (zonedDateTime.getMinute() == 0) {
                return "ha";
            }
            return "h:mma";
        } else if (isBothDateAndTime) {
            return "dd MMMM yyyy HH:mm:ss";
        }
        return "dd MMMM yyyy";
    }

    public static String formatLocalDateTimeToBst(LocalDateTime date) {
        return date.format(
            DateTimeFormatter.ofPattern("dd MMMM yyyy"));
    }

    public static ZonedDateTime convertStringToBst(String timestamp) {
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
        }
        return "";
    }

    private static String formatDurationTime(int duration, String format) {
        if (duration > ONE) {
            return duration + " " + format + "s";
        }
        return duration + " " + format;
    }

}
