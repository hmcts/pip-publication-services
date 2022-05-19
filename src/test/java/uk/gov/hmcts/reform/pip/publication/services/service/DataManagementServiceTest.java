package uk.gov.hmcts.reform.pip.publication.services.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.client.WebClientConfigurationTest;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {Application.class, WebClientConfigurationTest.class})
@ActiveProfiles("test")
public class DataManagementServiceTest {

    private static MockWebServer mockPublicationServicesEndpoint;

    @Autowired
    WebClient webClient;

    @Autowired
    DataManagementService dataManagementService;

    @BeforeEach
    public void setup() throws IOException {
        mockPublicationServicesEndpoint = new MockWebServer();
        mockPublicationServicesEndpoint.start(8081);
    }

    @AfterEach
    public void after() throws IOException {
        mockPublicationServicesEndpoint.close();
    }

    @Test
    void testGetArtefactReturnsOk() throws IOException {
        UUID uuid = UUID.randomUUID();
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader("Content-Type",
                                                               ContentType.APPLICATION_JSON)
                                                     .setBody("{\"artefactId\": \"" + uuid + "\"}")
                                                    .setResponseCode(200));

        Artefact artefact = dataManagementService.getArtefact(uuid);
        assertEquals(uuid, artefact.getArtefactId(), "Returned artefact does not match expected artefact");
    }

    @Test
    void testGetArtefactReturnsException() throws IOException {
        UUID uuid = UUID.randomUUID();

        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        NotifyException notifyException = assertThrows(NotifyException.class, () -> dataManagementService.getArtefact(uuid),
                     "Expected exception has not been thrown");

        assertTrue(notifyException.getMessage().contains("404"),
                   "Exception response does not contain the status code in the message");
    }

    @Test
    void testGetLocationReturnsOk() throws IOException {
        String locationName = "locationName";

        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader("Content-Type",
                                                               ContentType.APPLICATION_JSON)
                                                    .setBody("{\"name\": \"" + locationName + "\"}")
                                                    .setResponseCode(200));

        Location location = dataManagementService.getLocation("1234");
        assertEquals(locationName, location.getName(), "Returned location does not match expected location");
    }

    @Test
    void testGetLocationReturnsException() throws IOException {
        String locationId = "1234";

        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        NotifyException notifyException = assertThrows(NotifyException.class, () -> dataManagementService.getLocation(locationId),
                                                       "Expected exception has not been thrown");

        assertTrue(notifyException.getMessage().contains("404"),
                   "Exception response does not contain the status code in the message");
    }

    @Test
    void testGetArtefactFlatFileReturnsOk() throws IOException {
        UUID artefactId = UUID.randomUUID();
        String responseBody = "responseBody";

        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader("Content-Type",
                                                               ContentType.APPLICATION_OCTET_STREAM)
                                                    .setBody(responseBody)
                                                    .setResponseCode(200));

        byte[] returnedContent = dataManagementService.getArtefactFlatFile(artefactId);
        assertEquals(responseBody, new String(returnedContent), "Returned file content does" +
            " not match expected file content");
    }

    @Test
    void testGetArtefactContentReturnsException() throws IOException {
        UUID uuid = UUID.randomUUID();

        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        NotifyException notifyException = assertThrows(NotifyException.class, () -> dataManagementService.getArtefactFlatFile(uuid),
                                                       "Expected exception has not been thrown");

        assertTrue(notifyException.getMessage().contains("404"),
                   "Exception response does not contain the status code in the message");
    }



}
