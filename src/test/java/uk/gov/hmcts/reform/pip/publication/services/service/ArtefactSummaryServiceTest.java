package uk.gov.hmcts.reform.pip.publication.services.service;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ListType;
import uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary.ArtefactSummaryService;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ArtefactSummaryServiceTest {

    @Autowired
    ArtefactSummaryService artefactSummaryService;

    private static final String BODY_WRONG = "Body is not as expected.";
    private static final String MISSING_DATA_RETURN = "Data expected in the returned summary data did not arrive.";
    private static final String STRING_NOT_EMPTY = "The returned string should trigger the default (i.e. empty string)";
    private static final String MOCKS_DIR = "src/test/resources/mocks/";

    @Test
    void civilDailyCauseList() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get(MOCKS_DIR, "civilDailyCauseList"
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
        IOUtils.copy(Files.newInputStream(Paths.get(MOCKS_DIR, "sjpPressList.json")),
                     writer, Charset.defaultCharset()
        );
        String body = writer.toString();
        assertThat(body).as(BODY_WRONG).contains("Grand Theft Auto");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.SJP_PRESS_LIST)).as(MISSING_DATA_RETURN)
            .contains("Swampy Jorts");
    }

    @Test
    void sjpPublicList() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get(MOCKS_DIR, "SjpPublicList.json")),
                     writer, Charset.defaultCharset()
        );
        String body = writer.toString();
        assertThat(body).as(BODY_WRONG).contains("This is a middle name2");

        assertThat(artefactSummaryService.artefactSummary(body, ListType.SJP_PUBLIC_LIST)).as(MISSING_DATA_RETURN)
            .contains("This is an organisation2");
    }

    @Test
    void familyDailyCauseList() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get(MOCKS_DIR, "familyDailyCauseList.json")),
                     writer, Charset.defaultCharset()
        );
        String body = writer.toString();
        assertThat(body).as(BODY_WRONG).contains("Re: A Minor");

        assertThat(artefactSummaryService.artefactSummary(body, ListType.FAMILY_DAILY_CAUSE_LIST))
            .as(MISSING_DATA_RETURN).contains("fam_cause");
    }

    @Test
    void magsPublicList() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get(MOCKS_DIR, "familyDailyCauseList.json")),
                     writer, Charset.defaultCharset()
        );
        String body = writer.toString();
        assertThat(artefactSummaryService.artefactSummary(body, ListType.MAGS_PUBLIC_LIST)).as(STRING_NOT_EMPTY)
            .hasSize(0);
    }
}
