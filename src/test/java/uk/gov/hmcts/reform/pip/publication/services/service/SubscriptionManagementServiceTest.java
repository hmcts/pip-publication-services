package uk.gov.hmcts.reform.pip.publication.services.service;

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
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientConfigurationTest;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {Application.class, WebClientConfigurationTest.class})
@ActiveProfiles("test")
class SubscriptionManagementServiceTest {
    private static final String RESPONSE_BODY = "responseBody";
    private static final String NOT_FOUND = "404";
    private static final String RESPONSE_NOT_MATCH = "Response does not match";
    private static final String EXCEPTION_NOT_MATCH = "Exception does not match";
    private static final String MESSAGE_NOT_MATCH = "Message does not match";

    private static MockWebServer mockPublicationServicesEndpoint;

    @Autowired
    WebClient webClient;

    @Autowired
    SubscriptionManagementService subscriptionManagementService;

    @BeforeEach
    void setup() throws IOException {
        mockPublicationServicesEndpoint = new MockWebServer();
        mockPublicationServicesEndpoint.start(8081);
    }

    @AfterEach
    void after() throws IOException {
        mockPublicationServicesEndpoint.close();
    }

    @Test
    void testGetAllMiDataReturnsOk() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .setBody(RESPONSE_BODY)
                                                    .setResponseCode(200));

        String response = subscriptionManagementService.getAllMiData();
        assertEquals(RESPONSE_BODY, response, RESPONSE_NOT_MATCH);
    }

    @Test
    void testGetAllMiDataThrowsException() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class, () ->
                                                                     subscriptionManagementService.getAllMiData(),
                                                                 EXCEPTION_NOT_MATCH);

        assertTrue(notifyException.getMessage().contains(NOT_FOUND), MESSAGE_NOT_MATCH);
    }

    @Test
    void testGetLocationMiDataReturnsOk() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .setBody(RESPONSE_BODY)
                                                    .setResponseCode(200));

        String response = subscriptionManagementService.getLocationMiData();
        assertEquals(RESPONSE_BODY, response, RESPONSE_NOT_MATCH);
    }

    @Test
    void testGetLocationMiDataThrowsException() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class, () ->
                                                                     subscriptionManagementService.getLocationMiData(),
                                                                 EXCEPTION_NOT_MATCH);

        assertTrue(notifyException.getMessage().contains(NOT_FOUND), MESSAGE_NOT_MATCH);
    }
}
