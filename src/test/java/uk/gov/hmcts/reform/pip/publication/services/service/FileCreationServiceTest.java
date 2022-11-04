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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    private static final byte[] TEST_BYTE = "Test byte".getBytes();

    private static final List<MediaApplication> MEDIA_APPLICATION_LIST = List.of(new MediaApplication(
        UUID.randomUUID(), "Test user", "test@email.com", "Test employer",
        UUID.randomUUID().toString(), "test-image.png", LocalDateTime.now(),
        "REJECTED", LocalDateTime.now()
    ));

    @Test
    void testCreateMediaApplicationReportingCsvSuccess() {
        assertNotNull(
            fileCreationService.createMediaApplicationReportingCsv(MEDIA_APPLICATION_LIST),
            "Csv creation should not return null"
        );
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
