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
class CopDailyCauseListTest {

    @Autowired
    CopDailyCauseList copDailyCauseList;

    @Test
    void testCopDailyCauseListTemplate() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get(
            "src/test/resources/mocks/copDailyCauseList.json"
                     )), writer, Charset.defaultCharset()
        );

        String emailOutput = copDailyCauseList.artefactSummaryCopDailyCauseList(writer.toString());

        assertThat(emailOutput)
            .as("incorrect case suppression name found")
            .contains("ThisIsACaseSupressionName");

        assertThat(emailOutput)
            .as("incorrect case ID found")
            .contains("12341234");

        assertThat(emailOutput)
            .as("incorrect hearing found")
            .contains("Criminal");

        assertThat(emailOutput)
            .as("incorrect location found")
            .contains("Teams, In-Person");

        assertThat(emailOutput)
            .as("incorrect duration found")
            .contains("1 hour [1 of 2]");

        assertThat(emailOutput)
            .as("incorrect judge found")
            .contains("Mrs Firstname Surname");
    }

}
