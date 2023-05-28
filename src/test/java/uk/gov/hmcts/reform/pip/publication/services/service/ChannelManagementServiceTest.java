package uk.gov.hmcts.reform.pip.publication.services.service;

import com.azure.core.http.ContentType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.model.publication.FileType;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientTestConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {Application.class, WebClientTestConfiguration.class})
@ActiveProfiles("test")
class ChannelManagementServiceTest {

    private static MockWebServer mockChannelManagementMockEndpoint;
    private final ObjectWriter ow = new ObjectMapper().findAndRegisterModules().writer().withDefaultPrettyPrinter();
    private static final String HELLO = "hello";

    @Autowired
    WebClient webClient;

    @Autowired
    ChannelManagementService channelManagementService;

    @BeforeEach
    void setup() throws IOException {
        mockChannelManagementMockEndpoint = new MockWebServer();
        mockChannelManagementMockEndpoint.start(8181);
    }

    @AfterEach
    void teardown() throws IOException {
        mockChannelManagementMockEndpoint.shutdown();
    }

    @Test
    void testGetArtefactSummary() {
        mockChannelManagementMockEndpoint.enqueue(new MockResponse().addHeader(
            "Content-Type",
            ContentType.APPLICATION_JSON
        ).setBody(HELLO));

        String returnedString = channelManagementService.getArtefactSummary(UUID.randomUUID());

        assertEquals(HELLO, returnedString, "Return does not match");
    }

    @Test
    void testGetArtefactSummaryError() {
        mockChannelManagementMockEndpoint.enqueue(new MockResponse().setResponseCode(404));

        UUID artefactId = UUID.randomUUID();
        ServiceToServiceException exception = assertThrows(ServiceToServiceException.class,
            () -> channelManagementService.getArtefactSummary(artefactId), "Exception");

        assertTrue(exception.getMessage().contains("404"), "Exception didn't contain correct message");
    }

    @Test
    void testGetArtefactFile() throws JsonProcessingException {
        mockChannelManagementMockEndpoint.enqueue(
            new MockResponse()
                .addHeader("Content-Type", ContentType.APPLICATION_JSON)
                .setBody(ow.writeValueAsString(HELLO))
        );

        String response = channelManagementService.getArtefactFile(UUID.randomUUID(), FileType.PDF);
        assertTrue(response.length() > 0, "Response doesn't exist");
    }

    @Test
    void testGetArtefactFileNotFound() {
        mockChannelManagementMockEndpoint.enqueue(new MockResponse().setResponseCode(404));

        String response = channelManagementService.getArtefactFile(UUID.randomUUID(), FileType.EXCEL);
        assertTrue(response.length() == 0, "Response not empty");
    }

    @Test
    void testGetArtefactFileTooLarge() {
        mockChannelManagementMockEndpoint.enqueue(new MockResponse().setResponseCode(413));

        String response = channelManagementService.getArtefactFile(UUID.randomUUID(), FileType.PDF);
        assertTrue(response.length() == 0, "Response not empty");
    }

    @Test
    void testGetArtefactFileError() {
        mockChannelManagementMockEndpoint.enqueue(new MockResponse().setResponseCode(500));

        UUID artefactId = UUID.randomUUID();
        ServiceToServiceException exception = assertThrows(ServiceToServiceException.class, () ->
            channelManagementService.getArtefactFile(artefactId, FileType.PDF), "Exception");

        assertTrue(exception.getMessage().contains("500"), "Exception didn't contain correct message");
    }
}
