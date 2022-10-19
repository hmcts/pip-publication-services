package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class IacDailyListTests {

    @Test
    void testIacDailyListTemplate() throws IOException {

        IacDailyList iacDailyList = new IacDailyList();

        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get(
                         "src/test/resources/mocks/",
                         "iacDailyList.json"
                     )), writer,
                     Charset.defaultCharset()
        );

        String artefactSummary = iacDailyList.artefactSummary(writer.toString());

        assertThat(artefactSummary.split(System.lineSeparator()))
            .as("Incorrect output lines")
                .hasSize(14);

        assertThat(artefactSummary)
            .as("incorrect start time found")
            .contains("9pm");

        assertThat(artefactSummary)
            .as("incorrect case ref found")
            .contains("12341234");

        assertThat(artefactSummary)
            .as("incorrect hearing channel found")
            .contains("Teams, Attended");

        assertThat(artefactSummary)
            .as("incorrect claimant found")
            .contains("Surname");

        assertThat(artefactSummary)
            .as("incorrect prosecuting authority found")
            .contains("Authority Surname");
    }

}
