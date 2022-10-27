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
@SuppressWarnings("PMD.TooManyMethods")
class ArtefactSummaryServiceTest {

    @Autowired
    ArtefactSummaryService artefactSummaryService;

    private static final String MISSING_DATA_RETURN = "Data expected in the returned summary data did not arrive.";
    private static final String BODY_WRONG = "Body is not as expected.";
    private static final String STRING_NOT_EMPTY = "The returned string should trigger the default (i.e. empty string)";
    private static final String NULL_FILE = "Mock file is null - are you sure it's still there?";

    private static final String CASE_ID = "12341234";

    private String readMockJsonFile(String filePath)  throws IOException {
        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(filePath)) {
            return new String(mockFile.readAllBytes());
        }
    }

    @Test
    void notImplementedListTest() throws IOException {
        String body = readMockJsonFile("mocks/crownDailyList.json");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.CROWN_FIRM_LIST))
            .as(MISSING_DATA_RETURN).isEqualTo("");
    }

    @Test
    void crownDailyList() throws IOException {
        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("mocks/crownDailyList.json")) {
            assertThat(mockFile).as(NULL_FILE).isNotNull();
            String body = new String(mockFile.readAllBytes());
            assertThat(body).as(BODY_WRONG).contains("sitting");
            assertThat(artefactSummaryService.artefactSummary(
                body,
                ListType.CROWN_DAILY_LIST
            )).as(MISSING_DATA_RETURN).contains(
                "Case Reference - 12345678");
        }
    }

    @Test
    void civilDailyCauseList() throws IOException {
        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("mocks/civilDailyCauseList.json")) {
            assertThat(mockFile).as(NULL_FILE).isNotNull();
            String body = new String(mockFile.readAllBytes());
            assertThat(body).as(BODY_WRONG).contains("sitting");
            assertThat(artefactSummaryService.artefactSummary(
                body,
                ListType.CIVIL_DAILY_CAUSE_LIST
            )).as(MISSING_DATA_RETURN).contains(
                "Hearing Type: FHDRA (First Hearing and Dispute Resolution Appointment)");
        }
    }

    @Test
    void sjpPressList() throws IOException {
        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("mocks/sjpPressList.json")) {
            assertThat(mockFile).as(NULL_FILE).isNotNull();
            String body = new String(mockFile.readAllBytes());
            assertThat(body).as(BODY_WRONG).contains("Grand Theft Auto");
            assertThat(artefactSummaryService.artefactSummary(body, ListType.SJP_PRESS_LIST)).as(MISSING_DATA_RETURN)
                .contains("Swampy Jorts");
        }
    }

    @Test
    void sjpPublicList() throws IOException {
        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("mocks/sjpPublicList.json")) {
            assertThat(mockFile).as(NULL_FILE).isNotNull();
            String body = new String(mockFile.readAllBytes());
            assertThat(body).as(BODY_WRONG).contains("This is a middle name2");
            assertThat(artefactSummaryService.artefactSummary(body, ListType.SJP_PUBLIC_LIST)).as(MISSING_DATA_RETURN)
                .contains("This is an organisation2");
        }
    }

    @Test
    void sscsDailyList() throws IOException {
        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("mocks/sscsDailyList.json")) {
            assertThat(mockFile).as(NULL_FILE).isNotNull();
            String body = new String(mockFile.readAllBytes());
            assertThat(body).as(BODY_WRONG).contains("Aguilera");
            assertThat(artefactSummaryService.artefactSummary(body, ListType.SSCS_DAILY_LIST)).as(MISSING_DATA_RETURN)
                .contains("Troy F McClure");
        }
    }


    @Test
    void familyDailyCauseListTest() throws IOException {
        String body = readMockJsonFile("mocks/familyDailyCauseList.json");
        assertThat(body).as(BODY_WRONG).contains("AA1 AA1");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.FAMILY_DAILY_CAUSE_LIST))
            .as(MISSING_DATA_RETURN).contains(CASE_ID);
    }

    @Test
    void civilAndFamilyDailyCauseListTest() throws IOException {
        String body = readMockJsonFile("mocks/civilAndFamilyDailyCauseList.json");
        assertThat(body).as(BODY_WRONG).contains("AA1 AA1");
        assertThat(artefactSummaryService.artefactSummary(body,
                                                          ListType.CIVIL_AND_FAMILY_DAILY_CAUSE_LIST))
            .as(MISSING_DATA_RETURN).contains(CASE_ID);
    }

    @Test
    void copDailyCauseListTest() throws IOException {
        String body = readMockJsonFile("mocks/copDailyCauseList.json");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.COP_DAILY_CAUSE_LIST))
            .as(MISSING_DATA_RETURN).contains(CASE_ID);
    }

    @Test
    void primaryHealthListTest() throws IOException {
        String body = readMockJsonFile("mocks/primaryHealthList.json");
        assertThat(body).as(BODY_WRONG).contains("A Vs B");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.PRIMARY_HEALTH_LIST))
            .as(MISSING_DATA_RETURN).contains("A Vs B");
    }

    @Test
    void magsPublicListTest() throws IOException {
        String body = readMockJsonFile("mocks/familyDailyCauseList.json");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.MAGISTRATES_PUBLIC_LIST)).as(STRING_NOT_EMPTY)
            .hasSize(0);
    }

    @Test
    void iacDailyListTest() throws IOException {
        String body = readMockJsonFile("mocks/iacDailyList.json");
        assertThat(body).as(BODY_WRONG).contains("12341234");
        assertThat(artefactSummaryService.artefactSummary(body, ListType.IAC_DAILY_LIST))
            .as(MISSING_DATA_RETURN).contains(CASE_ID);
    }

    @Test
    void magsPublicList() throws IOException {
        try (InputStream mockFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("mocks/familyDailyCauseList.json")) {
            assertThat(mockFile).as(NULL_FILE).isNotNull();
            String body = new String(mockFile.readAllBytes());
            assertThat(artefactSummaryService.artefactSummary(body, ListType.MAGISTRATES_PUBLIC_LIST))
                .as(STRING_NOT_EMPTY).hasSize(0);
        }
    }
}
