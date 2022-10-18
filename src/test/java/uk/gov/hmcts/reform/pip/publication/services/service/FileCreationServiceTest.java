package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class FileCreationServiceTest {

    @InjectMocks
    private FileCreationService fileCreationService;

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
}
