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
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientTestConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;
import uk.gov.hmcts.reform.pip.publication.services.models.external.FileType;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

        ServiceToServiceException exception = assertThrows(ServiceToServiceException.class,
            () -> channelManagementService.getArtefactSummary(UUID.randomUUID()), "Exception");

        assertTrue(exception.getMessage().contains("404"), "Exception didn't contain correct message");
    }

    @Test
    void testGetArtefactFiles() throws JsonProcessingException {
        Map<FileType, byte[]> testMap = new ConcurrentHashMap<>();
        testMap.put(FileType.PDF, HELLO.getBytes());
        testMap.put(FileType.EXCEL, HELLO.getBytes());

        mockChannelManagementMockEndpoint.enqueue(new MockResponse().addHeader(
            "Content-Type",
            ContentType.APPLICATION_JSON
        ).setBody(ow.writeValueAsString(testMap)));

        Map<FileType, byte[]> returnedMap = channelManagementService.getArtefactFiles(UUID.randomUUID());

        assertTrue(returnedMap.get(FileType.PDF).length > 0, "Byte array doesn't exist");
        assertTrue(returnedMap.get(FileType.EXCEL).length > 0, "Byte array doesn't exist");
    }

    @Test
    void testGetArtefactFilesError() {
        mockChannelManagementMockEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ServiceToServiceException exception = assertThrows(ServiceToServiceException.class,
            () -> channelManagementService.getArtefactFiles(UUID.randomUUID()), "Exception");

        assertTrue(exception.getMessage().contains("404"), "Exception didn't contain correct message");
    }

}
