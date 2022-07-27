package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.service.helpers.Helpers;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("PMD.TooManyMethods")
@ActiveProfiles("test")
class HelpersTest {
    private static final String ERR_MSG = "Helper method doesn't seem to be working correctly";
    private static final String TEST = "test";
    private static JsonNode inputJson;

    @BeforeAll
    public static void setup()  throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/", "familyDailyCauseList.json")), writer,
                     Charset.defaultCharset()
        );

        inputJson = new ObjectMapper().readTree(writer.toString());
    }

    @Test
    void testLocalTimeMethod() {
        assertThat(Helpers.formatLocalDateTimeToBst(
            LocalDateTime.of(1988, Month.SEPTEMBER, 29, 8, 30)))
            .as(ERR_MSG)
            .isEqualTo("29 September 1988");
    }

    @Test
    void testZonedDateMethod() {
        assertThat(Helpers.formatTimeStampToBst(
            "2022-07-26T16:04:43.416924Z", false, false))
            .as(ERR_MSG)
            .isEqualTo("26 July 2022");
    }

    @Test
    void testZonedTimeMethod() {
        assertThat(Helpers.formatTimeStampToBst(
            "2022-07-26T16:04:43.416924Z", true, false))
            .as(ERR_MSG)
            .isEqualTo("5:04pm");
    }

    @Test
    void testZonedTimeOnlyHoursMethod() {
        assertThat(Helpers.formatTimeStampToBst(
            "2022-07-26T16:00:00.416924Z", true, false))
            .as(ERR_MSG)
            .isEqualTo("5pm");
    }

    @Test
    void testZonedTimeOnlyTwoDigitsHoursMethod() {
        assertThat(Helpers.formatTimeStampToBst(
            "2022-07-26T22:00:00.416924Z", true, false))
            .as(ERR_MSG)
            .isEqualTo("11pm");
    }

    @Test
    void testZonedDateTimeMethod() {
        assertThat(Helpers.formatTimeStampToBst(
            "2022-07-26T16:04:43.416924Z", false, true))
            .as(ERR_MSG)
            .isEqualTo("26 July 2022 17:04:43");
    }

    @Test
    void testConvertStringToUtcMethod() {
        assertThat(Helpers.convertStringToUtc("2022-08-19T09:30:00Z").toLocalDateTime())
            .as(ERR_MSG)
            .isEqualTo("2022-08-19T10:30");
    }

    @Test
    void testStringDelimiterWithEmptyStringMethod() {
        assertThat(Helpers.stringDelimiter("", ","))
            .as(ERR_MSG)
            .isEqualTo("");
    }

    @Test
    void testStringDelimiterWithoutEmptyStringMethod() {
        assertThat(Helpers.stringDelimiter(TEST, ","))
            .as(ERR_MSG)
            .isEqualTo(",");
    }

    @Test
    void testFindAndReturnNodeTextMethod() {
        assertThat(Helpers.findAndReturnNodeText(inputJson.get("document"), "publicationDate"))
            .as(ERR_MSG)
            .isEqualTo("2022-07-21T14:01:43Z");
    }

    @Test
    void testFindAndReturnNodeTextNotExistsMethod() {
        assertThat(Helpers.findAndReturnNodeText(inputJson.get("document"), TEST))
            .as(ERR_MSG)
            .isEqualTo("");
    }

    @Test
    void testTrimAnyCharacterFromStringEndMethod() {
        assertThat(Helpers.trimAnyCharacterFromStringEnd("test,"))
            .as(ERR_MSG)
            .isEqualTo(TEST);
    }

    @Test
    void testTrimAnyCharacterFromStringWithSpaceEndMethod() {
        assertThat(Helpers.trimAnyCharacterFromStringEnd("test, "))
            .as(ERR_MSG)
            .isEqualTo(TEST);
    }

    @Test
    void testConvertTimeToMinutesMethod() {
        ZonedDateTime startTime = Helpers.convertStringToUtc("2022-08-19T09:30:00Z");
        ZonedDateTime endTime = Helpers.convertStringToUtc("2022-08-19T10:55:00Z");

        assertThat(Helpers.convertTimeToMinutes(startTime, endTime))
            .as(ERR_MSG)
            .isEqualTo(85);
    }

    @Test
    void testFormatDurationMethod() {
        assertThat(Helpers.formatDuration(3, 10))
            .as(ERR_MSG)
            .isEqualTo("3 hours 10 mins");
    }

    @Test
    void testFormatDurationWithNoMinutesMethod() {
        assertThat(Helpers.formatDuration(3, 0))
            .as(ERR_MSG)
            .isEqualTo("3 hours");
    }

    @Test
    void testFormatDurationWithSingleHourNoMinutesMethod() {
        assertThat(Helpers.formatDuration(1, 0))
            .as(ERR_MSG)
            .isEqualTo("1 hour");
    }

    @Test
    void testFormatDurationWithNoHourMethod() {
        assertThat(Helpers.formatDuration(0, 30))
            .as(ERR_MSG)
            .isEqualTo("30 mins");
    }

    @Test
    void testFormatDurationWithSingleMinuteNoHourMethod() {
        assertThat(Helpers.formatDuration(0, 1))
            .as(ERR_MSG)
            .isEqualTo("1 min");
    }

    @Test
    void testFormatDurationWithNoMinuteNoHourMethod() {
        assertThat(Helpers.formatDuration(0, 0))
            .as(ERR_MSG)
            .isEqualTo("");
    }
}
