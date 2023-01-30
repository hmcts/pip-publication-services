package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.ExcelGenerationService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings("PMD.ExcessiveImports")
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
}
