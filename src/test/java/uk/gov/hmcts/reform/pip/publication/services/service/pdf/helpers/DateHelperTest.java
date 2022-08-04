package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
class DateHelperTest {

    private static final String ERR_MSG = "Helper method doesn't seem to be working correctly";

    @Test
    void testLocalTimeMethod() {
        assertThat(DateHelper.formatLocalDateTimeToBst(
            LocalDateTime.of(1988, Month.SEPTEMBER, 29, 8, 30)))
            .as(ERR_MSG)
            .isEqualTo("29 September 1988");
    }

    @Test
    void testZonedDateMethod() {
        assertThat(DateHelper.formatTimeStampToBst(
            "2022-07-26T16:04:43.416924Z", false, false))
            .as(ERR_MSG)
            .isEqualTo("26 July 2022");
    }

    @Test
    void testZonedTimeOnlyHoursMethod() {
        assertThat(DateHelper.formatTimeStampToBst(
            "2022-07-26T16:00:00.416924Z", true, false))
            .as(ERR_MSG)
            .contains("5");
    }

    @Test
    void testZonedTimeOnlyTwoDigitsHoursMethod() {
        assertThat(DateHelper.formatTimeStampToBst(
            "2022-07-26T22:00:00.416924Z", true, false))
            .as(ERR_MSG)
            .contains("11");
    }

    @Test
    void testZonedDateTimeMethod() {
        assertThat(DateHelper.formatTimeStampToBst(
            "2022-07-26T16:04:43.416924Z", false, true))
            .as(ERR_MSG)
            .isEqualTo("26 July 2022 at 17:04");
    }

    @Test
    void testConvertStringToBstMethod() {
        assertThat(DateHelper.convertStringToBst("2022-08-19T09:30:00Z").toLocalDateTime())
            .as(ERR_MSG)
            .isEqualTo("2022-08-19T10:30");
    }

    @Test
    void testConvertTimeToMinutesMethod() {
        ZonedDateTime startTime = DateHelper.convertStringToBst("2022-08-19T09:30:00Z");
        ZonedDateTime endTime = DateHelper.convertStringToBst("2022-08-19T10:55:00Z");

        assertThat(DateHelper.convertTimeToMinutes(startTime, endTime))
            .as(ERR_MSG)
            .isEqualTo(85);
    }

    @Test
    void testFormatDurationMethod() {
        assertThat(DateHelper.formatDuration(3, 10))
            .as(ERR_MSG)
            .isEqualTo("3 hours 10 mins");
    }

    @Test
    void testFormatDurationWithNoMinutesMethod() {
        assertThat(DateHelper.formatDuration(3, 0))
            .as(ERR_MSG)
            .isEqualTo("3 hours");
    }

    @Test
    void testFormatDurationWithSingleHourNoMinutesMethod() {
        assertThat(DateHelper.formatDuration(1, 0))
            .as(ERR_MSG)
            .isEqualTo("1 hour");
    }

    @Test
    void testFormatDurationWithNoHourMethod() {
        assertThat(DateHelper.formatDuration(0, 30))
            .as(ERR_MSG)
            .isEqualTo("30 mins");
    }

    @Test
    void testFormatDurationWithNoHourAndOneMinuteMethod() {
        assertThat(DateHelper.formatDuration(0, 1))
            .as(ERR_MSG)
            .isEqualTo("1 min");
    }

    @Test
    void testFormatDurationWithSingleMinuteNoHourMethod() {
        assertThat(DateHelper.formatDuration(0, 1))
            .as(ERR_MSG)
            .isEqualTo("1 min");
    }

    @Test
    void testFormatDurationWithNoMinuteNoHourMethod() {
        assertThat(DateHelper.formatDuration(0, 0))
            .as(ERR_MSG)
            .isEmpty();
    }
}
