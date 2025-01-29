package uk.gov.hmcts.reform.pip.publication.services.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
class SubscriptionManagementServiceTest {
    private static final String NOT_FOUND = "404";
    private static final String EXCEPTION_NOT_MATCH = "Exception does not match";
    private static final String MESSAGE_NOT_MATCH = "Message does not match";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MockWebServer mockSubscriptionManagementEndpoint = new MockWebServer();

    SubscriptionManagementService subscriptionManagementService;

    @BeforeEach
    void setup() {
        WebClient mockedWebClient = WebClient.builder()
            .baseUrl(mockSubscriptionManagementEndpoint.url("/").toString())
            .build();
        subscriptionManagementService = new SubscriptionManagementService(mockedWebClient);
    }

    @AfterEach
    void after() throws IOException {
        mockSubscriptionManagementEndpoint.close();
    }

    @Test
    void testGetAllMiDataReturnsOk() throws JsonProcessingException {
        AllSubscriptionMiData data1 = new AllSubscriptionMiData();
        List<AllSubscriptionMiData> expectedData = List.of(data1);

        mockSubscriptionManagementEndpoint.enqueue(new MockResponse()
                                                       .addHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON)
                                                       .setBody(OBJECT_MAPPER.writeValueAsString(expectedData))
                                                       .setResponseCode(200));

        List<AllSubscriptionMiData> response = subscriptionManagementService.getAllMiData();

        assertEquals(expectedData, response, "Data does not match");
    }

    @Test
    void testGetAllMiDataThrowsException() {
        mockSubscriptionManagementEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class,
                                                                     subscriptionManagementService::getAllMiData,
                                                                 EXCEPTION_NOT_MATCH);

        assertTrue(notifyException.getMessage().contains(NOT_FOUND), MESSAGE_NOT_MATCH);
    }

    @Test
    void testGetLocationMiDataReturnsOk() throws JsonProcessingException {
        LocationSubscriptionMiData data1 = new LocationSubscriptionMiData();
        List<LocationSubscriptionMiData> expectedData = List.of(data1);

        mockSubscriptionManagementEndpoint.enqueue(new MockResponse()
                                                       .addHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON)
                                                       .setBody(OBJECT_MAPPER.writeValueAsString(expectedData))
                                                       .setResponseCode(200));

        List<LocationSubscriptionMiData> response = subscriptionManagementService.getLocationMiData();

        assertEquals(expectedData, response, "Data does not match");
    }

    @Test
    void testGetLocationMiDataThrowsException() {
        mockSubscriptionManagementEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class,
                                                                     subscriptionManagementService::getLocationMiData,
                                                                 EXCEPTION_NOT_MATCH);

        assertTrue(notifyException.getMessage().contains(NOT_FOUND), MESSAGE_NOT_MATCH);
    }
}
