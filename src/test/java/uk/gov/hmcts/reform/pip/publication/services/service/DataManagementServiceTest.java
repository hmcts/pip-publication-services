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
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientConfigurationTest;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {Application.class, WebClientConfigurationTest.class})
@ActiveProfiles("test")
class DataManagementServiceTest {

    private static final String RESPONSE_BODY = "responseBody";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String EXCEPTION_THROWN_MESSAGE = "Expected exception has not been thrown";
    private static final String EXCEPTION_RESPONSE_MESSAGE =
        "Exception response does not contain the status code in the message";

    private static MockWebServer mockPublicationServicesEndpoint;

    @Autowired
    WebClient webClient;

    @Autowired
    DataManagementService dataManagementService;

    private UUID uuid;

    @BeforeEach
    void setup() throws IOException {
        mockPublicationServicesEndpoint = new MockWebServer();
        mockPublicationServicesEndpoint.start(8081);

        uuid = UUID.randomUUID();
    }

    @AfterEach
    void after() throws IOException {
        mockPublicationServicesEndpoint.close();
    }

    @Test
    void testGetArtefactReturnsOk() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE_HEADER,
                                                               ContentType.APPLICATION_JSON)
                                                    .setBody("{\"artefactId\": \"" + uuid + "\"}")
                                                    .setResponseCode(200));

        Artefact artefact = dataManagementService.getArtefact(uuid);
        assertEquals(uuid, artefact.getArtefactId(), "Returned artefact does not match expected artefact");
    }

    @Test
    void testGetArtefactReturnsException() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(501));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class, () ->
                                                           dataManagementService.getArtefact(uuid),
                                                       EXCEPTION_THROWN_MESSAGE);

        assertTrue(notifyException.getMessage().contains("501"),
                   EXCEPTION_RESPONSE_MESSAGE);
    }

    @Test
    void testGetArtefactFlatFileReturnsOk() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE_HEADER,
                                                               ContentType.APPLICATION_OCTET_STREAM)
                                                    .setBody(RESPONSE_BODY)
                                                    .setResponseCode(200));

        byte[] returnedContent = dataManagementService.getArtefactFlatFile(uuid);
        assertEquals(RESPONSE_BODY, new String(returnedContent),
                     "Returned file content does not match expected file content");
    }

    @Test
    void testGetArtefactContentReturnsException() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(501));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class, () ->
                                                           dataManagementService.getArtefactFlatFile(uuid),
                                                       EXCEPTION_THROWN_MESSAGE);

        assertTrue(notifyException.getMessage().contains("501"),
                   EXCEPTION_RESPONSE_MESSAGE);
    }

    @Test
    void testGetLocationReturnsOk() throws IOException {
        String locationName = "locationName";

        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE_HEADER,
                                                               ContentType.APPLICATION_JSON)
                                                    .setBody("{\"name\": \"" + locationName + "\"}")
                                                    .setResponseCode(200));

        Location location = dataManagementService.getLocation("1234");
        assertEquals(locationName, location.getName(), "Returned location does not match expected location");
    }

    @Test
    void testGetLocationReturnsException() {
        String locationId = "1234";

        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class, () ->
                                                           dataManagementService.getLocation(locationId),
                                                       EXCEPTION_THROWN_MESSAGE);

        assertTrue(notifyException.getMessage().contains("404"),
                   EXCEPTION_RESPONSE_MESSAGE);
    }

    @Test
    void testGetArtefactJsonBlobReturnsOk() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE_HEADER,
                                                               ContentType.APPLICATION_JSON)
                                                    .setBody(RESPONSE_BODY)
                                                    .setResponseCode(200));

        String returnedContent = dataManagementService.getArtefactJsonBlob(uuid);
        assertEquals(RESPONSE_BODY, returnedContent, "Returned payload does not match expected data");
    }

    @Test
    void testGetArtefactJsonBlobThrowsException() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(501));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class, () ->
                                                                     dataManagementService.getArtefactFlatFile(uuid),
                                                                 EXCEPTION_THROWN_MESSAGE);

        assertTrue(notifyException.getMessage().contains("501"),
                   EXCEPTION_RESPONSE_MESSAGE);
    }

}
