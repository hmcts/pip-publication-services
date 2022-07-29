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

    private String readMockJsonFile(String filePath)  throws IOException {
        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(filePath)) {
            return new String(mockFile.readAllBytes());
        }
    }

    @Test
    void civilDailyCauseListTest() throws IOException {
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
}
