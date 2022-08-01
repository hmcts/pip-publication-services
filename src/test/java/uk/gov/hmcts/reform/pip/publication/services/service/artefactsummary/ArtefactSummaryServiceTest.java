package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ListType;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ArtefactSummaryServiceTest {

    @Autowired
    ArtefactSummaryService artefactSummaryService;

    private static final String MISSING_DATA_RETURN = "Data expected in the returned summary data did not arrive.";
    private static final String BODY_WRONG = "Body is not as expected.";
    private static final String STRING_NOT_EMPTY = "The returned string should trigger the default (i.e. empty string)";

    private String readMockJsonFile(String filePath)  throws IOException {
        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(filePath)) {
            return new String(mockFile.readAllBytes());
        }
    }

    @Test
    void copDailyCauseListTest() throws IOException {
        String body = readMockJsonFile("mocks/copDailyCauseList.json");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.COP_DAILY_CAUSE_LIST))
            .as(MISSING_DATA_RETURN).contains("12341234");
    }

    @Test
    void notImplementedListTest() throws IOException {
        String body = readMockJsonFile("mocks/copDailyCauseList.json");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.CROWN_DAILY_LIST))
            .as(MISSING_DATA_RETURN).isEqualTo("");
    }

    @Test
    void civilDailyCauseList() throws IOException {
        String body = readMockJsonFile("mocks/civilDailyCauseList.json");
        assertThat(body).as(BODY_WRONG).contains("sitting");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.CIVIL_DAILY_CAUSE_LIST))
            .as(MISSING_DATA_RETURN).contains("Hearing Type: Interim Third Party Order");

    }

    @Test
    void sjpPressList() throws IOException {
        String body = readMockJsonFile("mocks/sjpPressList.json");
        assertThat(body).as(BODY_WRONG).contains("Grand Theft Auto");

        assertThat(artefactSummaryService.artefactSummary(body, ListType.SJP_PRESS_LIST)).as(MISSING_DATA_RETURN)
                .contains("Swampy Jorts");

    }

    @Test
    void sjpPublicList() throws IOException {
        String body = readMockJsonFile("mocks/sjpPublicList.json");
        assertThat(body).as(BODY_WRONG).contains("This is a middle name2");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.SJP_PUBLIC_LIST)).as(MISSING_DATA_RETURN)
            .contains("This is an organisation2");

    }

    @Test
    void familyDailyCauseList() throws IOException {
        String body = readMockJsonFile("mocks/familyDailyCauseList.json");

        assertThat(body).as(BODY_WRONG).contains("Re: A Minor");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.FAMILY_DAILY_CAUSE_LIST))
                .as(MISSING_DATA_RETURN).contains("fam_cause");

    }

    @Test
    void magsPublicList() throws IOException {
        String body = readMockJsonFile("mocks/familyDailyCauseList.json");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.MAGS_PUBLIC_LIST)).as(STRING_NOT_EMPTY)
            .hasSize(0);
    }
}
