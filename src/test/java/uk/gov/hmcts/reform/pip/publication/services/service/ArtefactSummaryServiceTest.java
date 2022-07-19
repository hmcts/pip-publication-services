package uk.gov.hmcts.reform.pip.publication.services.service;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ListType;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ArtefactSummaryServiceTest {

    @Autowired
    ArtefactSummaryService artefactSummaryService;

    private static final String BODY_WRONG = "Body is not as expected.";
    private static final String MISSING_DATA_RETURN = "Data expected in the returned summary data did not arrive.";

    @Test
    void civilDailyCauseList() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/", "civilDailyCauseList"
                         + ".json")),
                     writer, Charset.defaultCharset()
        );
        String body = writer.toString();
        assertThat(body).as(BODY_WRONG).contains("sitting");
        assertThat(artefactSummaryService.artefactSummary(
            body,
            ListType.CIVIL_DAILY_CAUSE_LIST
        )).as(MISSING_DATA_RETURN).contains(
            "Hearing Type: Interim Third Party Order: ");
    }

    @Test
    void sjpPressList() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/", "sjpPressList.json")),
                     writer, Charset.defaultCharset()
        );
        String body = writer.toString();
        assertThat(body).contains("Grand Theft Auto").as(BODY_WRONG);
        assertThat(artefactSummaryService.artefactSummary(body, ListType.SJP_PRESS_LIST))
            .contains("Swampy Jorts").as(MISSING_DATA_RETURN);
    }

    @Test
    void sjpPublicList() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/", "sjpPublicList.json")),
                     writer, Charset.defaultCharset()
        );
        String body = writer.toString();
        assertThat(body).contains("This is a middle name2").as(BODY_WRONG);

        assertThat(artefactSummaryService.artefactSummary(body, ListType.SJP_PUBLIC_LIST))
            .contains("This is an organisation2").as(MISSING_DATA_RETURN);
    }


}
