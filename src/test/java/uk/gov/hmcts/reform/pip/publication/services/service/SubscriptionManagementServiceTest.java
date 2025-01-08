package uk.gov.hmcts.reform.pip.publication.services.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
class SubscriptionManagementServiceTest {
    private static final String RESPONSE_BODY = "responseBody";
    private static final String NOT_FOUND = "404";
    private static final String RESPONSE_NOT_MATCH = "Response does not match";
    private static final String EXCEPTION_NOT_MATCH = "Exception does not match";
    private static final String MESSAGE_NOT_MATCH = "Message does not match";

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
    void testGetAllMiDataReturnsOk() {
        mockSubscriptionManagementEndpoint.enqueue(new MockResponse()
                                                    .setBody(RESPONSE_BODY)
                                                    .setResponseCode(200));

        String response = subscriptionManagementService.getAllMiData();
        assertEquals(RESPONSE_BODY, response, RESPONSE_NOT_MATCH);
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
    void testGetLocationMiDataReturnsOk() {
        mockSubscriptionManagementEndpoint.enqueue(new MockResponse()
                                                    .setBody(RESPONSE_BODY)
                                                    .setResponseCode(200));

        String response = subscriptionManagementService.getLocationMiData();
        assertEquals(RESPONSE_BODY, response, RESPONSE_NOT_MATCH);
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
