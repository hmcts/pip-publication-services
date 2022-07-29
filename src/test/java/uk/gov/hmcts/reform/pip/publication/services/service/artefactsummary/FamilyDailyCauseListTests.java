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
class FamilyDailyCauseListTests {
    @Autowired
    DailyCauseList familyDailyCauseList;

    @Test
    void testFamilyCauseListTemplate() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get(
            "src/test/resources/mocks/",
            "familyDailyCauseList.json"
                     )), writer,
                     Charset.defaultCharset()
        );

        String emailOutput = familyDailyCauseList.artefactSummaryDailyCause(writer.toString());

        assertThat(emailOutput)
            .as("incorrect party name found")
            .contains("This is a case name [2 of 3]");

        assertThat(emailOutput)
            .as("incorrect case ID found")
            .contains("12341234");

        assertThat(emailOutput)
            .as("incorrect hearing found")
            .contains("Directions");

        assertThat(emailOutput)
            .as("incorrect location found")
            .contains("Teams, Attended");

        assertThat(emailOutput)
            .as("incorrect duration found")
            .contains("1 hour 25 mins");

        assertThat(emailOutput)
            .as("incorrect judge found")
            .contains("This is the court room name");
    }
}
