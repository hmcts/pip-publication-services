package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
class EtDailyListTest {
    @Test
    void testEtDailyListTemplate() throws IOException {
        EtDailyList etDailyList = new EtDailyList();

        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("mocks/etDailyList.json")) {
            String output = etDailyList.artefactSummaryEtDailyList(new String(mockFile.readAllBytes()));

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(output.split(System.lineSeparator()))
                .as("Incorrect output lines")
                .hasSize(40);

            softly.assertThat(output)
                .as("Incorrect start time")
                .contains("Start Time: 09:30am");

            softly.assertThat(output)
                .as("Incorrect duration")
                .contains("Duration: 2 hours");

            assertThat(output)
                .as("Incorrect case number")
                .contains("Case Number: 12341234");

            softly.assertThat(output)
                .as("Incorrect claimant")
                .contains("Claimant: HRH G Anderson, Rep: Mr R Hargreaves");

            softly.assertThat(output)
                .as("Incorrect respondent")
                .contains("Respondent: Capt. S Jenkins, Rep: Dr M Naylor");

            softly.assertThat(output)
                .as("Incorrect hearing type")
                .contains("Hearing Type: This is a hearing type");

            softly.assertThat(output)
                .as("Incorrect jurisdiction")
                .contains("Jurisdiction: This is a case type");

            softly.assertThat(output)
                .as("Incorrect hearing platform")
                .contains("Hearing Platform: This is a sitting channel");

            softly.assertAll();
        }
    }
}
