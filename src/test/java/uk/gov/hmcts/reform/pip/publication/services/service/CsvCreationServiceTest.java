package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class CsvCreationServiceTest {

    @Autowired
    private CsvCreationService csvCreationService;

    private static final List<MediaApplication> MEDIA_APPLICATION_LIST = List.of(new MediaApplication(
        UUID.randomUUID(), "Test user", "test@email.com", "Test employer",
        UUID.randomUUID().toString(), "test-image.png", LocalDateTime.now(),
        "REJECTED", LocalDateTime.now()));

    @Test
    void testCreateMediaApplicationReportingCsvSuccess() {
        assertNotNull(csvCreationService.createMediaApplicationReportingCsv(MEDIA_APPLICATION_LIST),
                      "Csv creation should not return null");
    }
}
