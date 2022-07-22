package uk.gov.hmcts.reform.pip.publication.services.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeHelperTest {
    @Test
    void testFormattingOfGmtDateTime() {
        assertThat(DateTimeHelper.formatZonedDateTime("2022-01-13T23:30:52.123Z"))
            .as("Incorrect date time format")
            .isEqualTo("13 January 2022 at 23:30");
    }

    @Test
    void testFormattingOfBstDateTime() {
        assertThat(DateTimeHelper.formatZonedDateTime("2021-08-31T08:45:52.123Z"))
            .as("Incorrect date time format")
            .isEqualTo("31 August 2021 at 09:45");
    }

    @Test
    void testDateFormatting() {
        LocalDateTime dateTime = LocalDateTime.of(2022, 1, 13, 0, 0, 0);
        assertThat(DateTimeHelper.formatDate(dateTime))
            .as("Incorrect date format")
            .isEqualTo("13 January 2022");
    }
}
