package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EtFortnightlyPressListTest {
    @Autowired
    EtFortnightlyPressList etFortnightlyPressList;

    @Test
    void testEtFortnightlyPressListTemplate() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get(
            "src/test/resources/mocks/",
            "etFortnightlyPressList.json"
                     )), writer,
                     Charset.defaultCharset()
        );

        String emailOutput = etFortnightlyPressList.artefactSummaryEtFortnightlyPressList(writer.toString());

        assertThat(emailOutput)
            .as("incorrect courtroom name found")
            .contains("Court 1");

        assertThat(emailOutput)
            .as("incorrect start time found")
            .contains("9:30am");

        assertThat(emailOutput)
            .as("incorrect duration found")
            .contains("2 hours [2 of 3]");

        assertThat(emailOutput)
            .as("incorrect case number found")
            .contains("12341234");

        assertThat(emailOutput)
            .as("incorrect Claimant found")
            .contains("HRH G Anderson");

        assertThat(emailOutput)
            .as("incorrect Respondent found")
            .contains("Capt. S Jenkins");

        assertThat(emailOutput)
            .as("incorrect hearing type found")
            .contains("This is a hearing type");

        assertThat(emailOutput)
            .as("incorrect Jurisdiction found")
            .contains("This is a case type");

        assertThat(emailOutput)
            .as("incorrect Hearing Platform found")
            .contains("This is a sitting channel");
    }
}
