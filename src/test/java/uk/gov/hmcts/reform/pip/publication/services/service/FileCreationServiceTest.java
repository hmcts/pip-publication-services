package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.ExcelGenerationService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class FileCreationServiceTest {

    @Mock
    private DataManagementService dataManagementService;

    @Mock
    private AccountManagementService accountManagementService;

    @Mock
    private SubscriptionManagementService subscriptionManagementService;

    @Mock
    private ExcelGenerationService excelGenerationService;

    @InjectMocks
    private FileCreationService fileCreationService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private static final byte[] TEST_BYTE = "Test byte".getBytes();
    private static final LocalDateTime REQUEST_DATE = LocalDateTime.now();
    private static final LocalDateTime STATUS_DATE = LocalDateTime.now();

    private static final List<MediaApplication> MEDIA_APPLICATION_LIST = List.of(new MediaApplication(
        UUID.randomUUID(), "Test user", "test@email.com", "Test employer",
        UUID.randomUUID().toString(), "test-image.png", REQUEST_DATE,
        "REJECTED", STATUS_DATE
    ));
    private static final String EXPECTED_HEADER = "\"Full name\",\"Email\",\"Employer\",\"Request date\","
        + "\"Status\",\"Status date\"";
    private static final String EXPECTED_CONTENT = "\"Test user\",\"test@email.com\",\"Test employer\",\""
        + REQUEST_DATE.format(DATE_TIME_FORMATTER) + "\",\"REJECTED\",\""
        + STATUS_DATE.format(DATE_TIME_FORMATTER) + "\"";

    private static final String PUBLICATION_MI_DATA = "Publications";
    private static final String ACCOUNT_MI_DATA = "User accounts";
    private static final String ALL_SUBSCRIPTION_MI_DATA = "All subscriptions";
    private static final String LOCATION_SUBSCRIPTION_MI_DATA = "Location subscriptions";
    private static final String MI_DATA_MATCH_MESSAGE = "MI data does not match";

    @Test
    void testCreateMediaApplicationReportingCsvSuccess() {
        byte[] result = fileCreationService.createMediaApplicationReportingCsv(MEDIA_APPLICATION_LIST);
        String[] csvResult = new String(result, StandardCharsets.UTF_8).split(System.lineSeparator());

        assertThat(csvResult)
            .as("CSV line count does not match")
            .hasSize(2);
        assertThat(csvResult[0])
            .as("CSV header does not match")
            .isEqualTo(EXPECTED_HEADER);
        assertThat(csvResult[1])
            .as("CSV content does not match")
            .isEqualTo(EXPECTED_CONTENT);
    }

    @Test
    void testGenerateMiReportSuccess() throws IOException {
        String data = "1,2,3";
        when(dataManagementService.getMiData()).thenReturn(data);
        when(accountManagementService.getMiData()).thenReturn(data);
        when(subscriptionManagementService.getAllMiData()).thenReturn(data);
        when(subscriptionManagementService.getLocationMiData()).thenReturn(data);
        when(excelGenerationService.generateMultiSheetWorkBook(any())).thenReturn(TEST_BYTE);

        assertThat(fileCreationService.generateMiReport()).isEqualTo(TEST_BYTE);
    }

    @Test
    void testExtractMiData() {
        when(dataManagementService.getMiData()).thenReturn("a,b,c\nd,e,f");
        when(accountManagementService.getMiData()).thenReturn("g,h,i\nj,k,l");
        when(subscriptionManagementService.getAllMiData()).thenReturn("m,n,o");
        when(subscriptionManagementService.getLocationMiData()).thenReturn(null);

        Map<String, List<String[]>> results = fileCreationService.extractMiData();

        assertThat(results)
            .as(MI_DATA_MATCH_MESSAGE)
            .containsKey(PUBLICATION_MI_DATA);

        assertThat(results)
            .as(MI_DATA_MATCH_MESSAGE)
            .containsKey(ACCOUNT_MI_DATA);

        assertThat(results)
            .as(MI_DATA_MATCH_MESSAGE)
            .containsKey(ALL_SUBSCRIPTION_MI_DATA);

        assertThat(results)
            .as(MI_DATA_MATCH_MESSAGE)
            .doesNotContainKey(LOCATION_SUBSCRIPTION_MI_DATA);

        List<String[]> publicationMiData = results.get(PUBLICATION_MI_DATA);
        assertThat(publicationMiData)
            .as(MI_DATA_MATCH_MESSAGE)
            .hasSize(2)
            .containsExactly(
                new String[]{"a", "b", "c"},
                new String[]{"d", "e", "f"}
            );

        List<String[]> accountMiData = results.get(ACCOUNT_MI_DATA);
        assertThat(accountMiData)
            .as(MI_DATA_MATCH_MESSAGE)
            .hasSize(2)
            .containsExactly(
                new String[]{"g", "h", "i"},
                new String[]{"j", "k", "l"}
            );

        List<String[]> allSubscriptionMiData = results.get(ALL_SUBSCRIPTION_MI_DATA);
        assertThat(allSubscriptionMiData)
            .as(MI_DATA_MATCH_MESSAGE)
            .hasSize(1)
            .containsExactly(
                new String[]{"m", "n", "o"}
            );
    }
}
