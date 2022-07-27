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

    @Test
    void familyDailyCauseListTest() throws IOException {
        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("mocks/familyDailyCauseList.json")) {
            String body = new String(mockFile.readAllBytes());
            assertThat(body).as(BODY_WRONG).contains("AA1 AA1");
            assertThat(artefactSummaryService.artefactSummary(body, ListType.FAMILY_DAILY_CAUSE_LIST))
                .as(MISSING_DATA_RETURN).contains("12341234");
        }
    }

    @Test
    void sjpPublicListTest() throws IOException {
        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("mocks/sjpPublicList.json")) {
            String body = new String(mockFile.readAllBytes());
            assertThat(artefactSummaryService.artefactSummary(body, ListType.SJP_PUBLIC_LIST))
                .as(MISSING_DATA_RETURN).contains("SJP PUBLIC LIST");
        }
    }

    @Test
    void sjpPressListTest() throws IOException {
        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("mocks/sjpPressList.json")) {
            String body = new String(mockFile.readAllBytes());
            assertThat(artefactSummaryService.artefactSummary(body, ListType.SJP_PRESS_LIST))
                .as(MISSING_DATA_RETURN).contains("SJP PRESS LIST");
        }
    }

    @Test
    void civilDailyCauseListTest() throws IOException {
        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("mocks/sjpPressList.json")) {
            String body = new String(mockFile.readAllBytes());
            assertThat(artefactSummaryService.artefactSummary(body, ListType.CIVIL_DAILY_CAUSE_LIST))
                .as(MISSING_DATA_RETURN).contains("CIVIL DAILY CAUSE LIST");
        }
    }

    @Test
    void notImplementedListTest() throws IOException {
        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("mocks/sjpPressList.json")) {
            String body = new String(mockFile.readAllBytes());
            assertThat(artefactSummaryService.artefactSummary(body, ListType.CROWN_DAILY_LIST))
                .as(MISSING_DATA_RETURN).isEqualTo("");
        }
    }
}
