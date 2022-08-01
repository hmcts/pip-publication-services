package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class HelpersTest {

    private static final String ERR_MSG = "Helper method doesn't seem to be working correctly";

    @Test
    void testLocalTimeMethod() {
        assertThat(Helpers.formatLocalDateTimeToBst(LocalDateTime.of(1988, Month.SEPTEMBER, 29, 8, 30)))
            .as(ERR_MSG)
            .isEqualTo("29 September 1988");
    }

    @Test
    void testZonedDateTimeMethod() {
        assertThat(Helpers.formatTimestampToBst("2022-07-26T16:04:43.416924Z"))
            .as(ERR_MSG)
            .isEqualTo("26 July 2022 at 17:04");
    }
}
