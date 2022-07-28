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

    private static final String BODY_WRONG = "Body is not as expected.";
    private static final String MISSING_DATA_RETURN = "Data expected in the returned summary data did not arrive.";

    private String readMockJsonFile(String filePath)  throws IOException {
        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(filePath)) {
            return new String(mockFile.readAllBytes());
        }
    }

    @Test
    void familyDailyCauseListTest() throws IOException {
        String body = readMockJsonFile("mocks/familyDailyCauseList.json");
        assertThat(body).as(BODY_WRONG).contains("AA1 AA1");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.FAMILY_DAILY_CAUSE_LIST))
            .as(MISSING_DATA_RETURN).contains("12341234");
    }

    @Test
    void sjpPublicListTest() throws IOException {
        String body = readMockJsonFile("mocks/sjpPublicList.json");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.SJP_PUBLIC_LIST))
           .as(MISSING_DATA_RETURN).contains("SJP PUBLIC LIST");
    }

    @Test
    void sjpPressListTest() throws IOException {
        String body = readMockJsonFile("mocks/sjpPressList.json");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.SJP_PRESS_LIST))
            .as(MISSING_DATA_RETURN).contains("SJP PRESS LIST");
    }

    @Test
    void civilDailyCauseListTest() throws IOException {
        String body = readMockJsonFile("mocks/civilDailyCauseList.json");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.CIVIL_DAILY_CAUSE_LIST))
            .as(MISSING_DATA_RETURN).contains("CIVIL DAILY CAUSE LIST");
    }

    @Test
    void notImplementedListTest() throws IOException {
        String body = readMockJsonFile("mocks/civilDailyCauseList.json");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.CROWN_DAILY_LIST))
           .as(MISSING_DATA_RETURN).isEqualTo("");
    }
}
