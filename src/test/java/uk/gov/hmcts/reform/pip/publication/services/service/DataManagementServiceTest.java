package uk.gov.hmcts.reform.pip.publication.services.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.FileType;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
class DataManagementServiceTest {
    private static final UUID ARTEFACT_ID = UUID.randomUUID();
    private static final String RESPONSE_BODY = "responseBody";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String EXCEPTION_THROWN_MESSAGE = "Expected exception has not been thrown";
    private static final String EXCEPTION_RESPONSE_MESSAGE =
        "Exception response does not contain the status code in the message";

    private final MockWebServer mockDataManagementEndpoint = new MockWebServer();

    private static final String NOT_FOUND = "404";
    private static final String NO_STATUS_CODE_IN_EXCEPTION = "Exception response does not contain the status code in"
        + " the message";
    private static final String NO_EXPECTED_EXCEPTION = "Expected exception has not been thrown.";
    private static final String HELLO = "hello";

    private final ObjectWriter ow = new ObjectMapper().findAndRegisterModules().writer().withDefaultPrettyPrinter();

    private DataManagementService dataManagementService;

    @BeforeEach
    void setup() {
        WebClient mockedWebClient = WebClient.builder()
            .baseUrl(mockDataManagementEndpoint.url("/").toString())
            .build();
        dataManagementService = new DataManagementService(mockedWebClient);
    }

    @AfterEach
    void after() throws IOException {
        mockDataManagementEndpoint.close();
    }

    @Test
    void testGetArtefactReturnsOk() {
        mockDataManagementEndpoint.enqueue(new MockResponse()
                                               .addHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON)
                                               .setBody("{\"artefactId\": \"" + ARTEFACT_ID + "\"}")
                                               .setResponseCode(200));

        Artefact artefact = dataManagementService.getArtefact(ARTEFACT_ID);
        assertEquals(ARTEFACT_ID, artefact.getArtefactId(), "Returned artefact does not match expected artefact");
    }

    @Test
    void testGetArtefactReturnsException() {
        mockDataManagementEndpoint.enqueue(new MockResponse().setResponseCode(501));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class, () ->
                                                           dataManagementService.getArtefact(ARTEFACT_ID),
                     NO_EXPECTED_EXCEPTION);

        assertTrue(notifyException.getMessage().contains("501"),
                   EXCEPTION_RESPONSE_MESSAGE);
    }

    @Test
    void testGetArtefactFlatFileReturnsOk() {
        mockDataManagementEndpoint.enqueue(new MockResponse()
                                               .addHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_OCTET_STREAM)
                                               .setBody(RESPONSE_BODY)
                                               .setResponseCode(200));

        byte[] returnedContent = dataManagementService.getArtefactFlatFile(ARTEFACT_ID);
        assertEquals(RESPONSE_BODY, new String(returnedContent),
                     "Returned file content does not match expected file content");
    }

    @Test
    void testGetArtefactContentReturnsException() {
        mockDataManagementEndpoint.enqueue(new MockResponse().setResponseCode(501));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class, () ->
                                                           dataManagementService.getArtefactFlatFile(ARTEFACT_ID),
                                                       EXCEPTION_THROWN_MESSAGE);

        assertTrue(notifyException.getMessage().contains("501"),
                   EXCEPTION_RESPONSE_MESSAGE);
    }

    @Test
    void testGetLocationReturnsOk() {
        String locationName = "locationName";

        mockDataManagementEndpoint.enqueue(new MockResponse()
                                               .addHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON)
                                               .setBody("{\"name\": \"" + locationName + "\"}")
                                               .setResponseCode(200));

        Location location = dataManagementService.getLocation("1234");
        assertEquals(locationName, location.getName(), "Returned location does not match expected location");
    }

    @Test
    void testGetLocationReturnsException() {
        String locationId = "1234";

        mockDataManagementEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class, () ->
                                                           dataManagementService.getLocation(locationId),
                                                       NO_EXPECTED_EXCEPTION);

        assertTrue(notifyException.getMessage().contains(NOT_FOUND),
                   NO_STATUS_CODE_IN_EXCEPTION);
    }

    @Test
    void testGetArtefactJsonBlobReturnsOk() {
        mockDataManagementEndpoint.enqueue(new MockResponse()
                                               .addHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON)
                                               .setBody(RESPONSE_BODY)
                                               .setResponseCode(200));

        String returnedContent = dataManagementService.getArtefactJsonBlob(ARTEFACT_ID);
        assertEquals(RESPONSE_BODY, returnedContent, "Returned payload does not match expected data");
    }

    @Test
    void testGetArtefactJsonBlobThrowsException() {
        mockDataManagementEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class, () ->
                                                           dataManagementService.getArtefactFlatFile(ARTEFACT_ID),
                                                       NO_EXPECTED_EXCEPTION);

        assertTrue(notifyException.getMessage().contains(NOT_FOUND),
                   NO_STATUS_CODE_IN_EXCEPTION);
    }


    @Test
    void testGetArtefactJsonPayload() {
        mockDataManagementEndpoint.enqueue(new MockResponse()
                                               .setBody("testJsonString")
                                               .setResponseCode(200));
        String jsonPayload = dataManagementService.getArtefactJsonBlob(ARTEFACT_ID);
        assertEquals("testJsonString", jsonPayload, "Messages do not match");
    }

    @Test
    void testFailedGetArtefactJsonPayload() {
        mockDataManagementEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class, () ->
                                                           dataManagementService.getArtefactJsonBlob(ARTEFACT_ID),
                                                       NO_EXPECTED_EXCEPTION);
        assertTrue(notifyException.getMessage().contains(NOT_FOUND),
                   NO_STATUS_CODE_IN_EXCEPTION);
    }

    @Test
    void testGetArtefactSummary() {
        mockDataManagementEndpoint.enqueue(new MockResponse().addHeader(
            CONTENT_TYPE_HEADER,
            com.azure.core.http.ContentType.APPLICATION_JSON
        ).setBody(HELLO));

        String returnedString = dataManagementService.getArtefactSummary(ARTEFACT_ID);

        assertEquals(HELLO, returnedString, "Return does not match");
    }

    @Test
    void testGetArtefactSummaryWhenNullReturnsEmptyString() {
        mockDataManagementEndpoint.enqueue(new MockResponse().addHeader(
            CONTENT_TYPE_HEADER,
            com.azure.core.http.ContentType.APPLICATION_JSON
        ));

        String returnedString = dataManagementService.getArtefactSummary(ARTEFACT_ID);

        assertEquals("", returnedString, "Return does not match");
    }

    @Test
    void testGetArtefactSummaryError() {
        mockDataManagementEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ServiceToServiceException exception = assertThrows(ServiceToServiceException.class,
                                                           () -> dataManagementService.getArtefactSummary(ARTEFACT_ID),
                                                           "Exception");

        assertTrue(exception.getMessage().contains("404"), "Exception didn't contain correct message");
    }

    @Test
    void testGetArtefactFile() throws JsonProcessingException {
        mockDataManagementEndpoint.enqueue(
            new MockResponse()
                .addHeader(CONTENT_TYPE_HEADER, com.azure.core.http.ContentType.APPLICATION_JSON)
                .setBody(ow.writeValueAsString(HELLO))
        );

        String response = dataManagementService.getArtefactFile(ARTEFACT_ID, FileType.PDF, true);
        assertTrue(response.length() > 0, "Response doesn't exist");
    }

    @Test
    void testGetArtefactFileNotFound() {
        mockDataManagementEndpoint.enqueue(new MockResponse().setResponseCode(404));

        String response = dataManagementService.getArtefactFile(ARTEFACT_ID, FileType.EXCEL, false);
        assertEquals(0, response.length(), "Response not empty");
    }

    @Test
    void testGetArtefactFileTooLarge() {
        mockDataManagementEndpoint.enqueue(new MockResponse().setResponseCode(413));

        String response = dataManagementService.getArtefactFile(ARTEFACT_ID, FileType.PDF, false);
        assertEquals(0, response.length(), "Response not empty");
    }

    @Test
    void testGetArtefactFileError() {
        mockDataManagementEndpoint.enqueue(new MockResponse().setResponseCode(500));

        ServiceToServiceException exception = assertThrows(ServiceToServiceException.class, () ->
            dataManagementService.getArtefactFile(ARTEFACT_ID, FileType.PDF, false), "Exception");

        assertTrue(exception.getMessage().contains("500"), "Exception didn't contain correct message");
    }

    @Test
    void testGetMiDataReturnsOk() {
        mockDataManagementEndpoint.enqueue(new MockResponse()
                                               .setBody(RESPONSE_BODY)
                                               .setResponseCode(200));

        String response = dataManagementService.getMiData();
        assertEquals(RESPONSE_BODY, response, "Messages do not match");
    }

    @Test
    void testGetMiDataThrowsException() {
        mockDataManagementEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class,
                                                                     dataManagementService::getMiData,
                                                                 NO_EXPECTED_EXCEPTION);

        assertTrue(notifyException.getMessage().contains(NOT_FOUND), NO_STATUS_CODE_IN_EXCEPTION);
    }
}
