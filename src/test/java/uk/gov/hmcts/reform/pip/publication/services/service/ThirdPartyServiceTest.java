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
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ThirdPartyServiceException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {Application.class, WebClientConfigurationTest.class})
@ActiveProfiles("test")
class ThirdPartyServiceTest {

    @Autowired
    private ThirdPartyService thirdPartyService;

    @Autowired
    WebClient.Builder webClient;

    private static final String API = "localhost:4444";
    private static final String PAYLOAD = "test payload";
    private static MockWebServer mockPublicationServicesEndpoint;

    @BeforeEach
    void setup() throws IOException {
        mockPublicationServicesEndpoint = new MockWebServer();
        mockPublicationServicesEndpoint.start(4444);
    }

    @AfterEach
    void after() throws IOException {
        mockPublicationServicesEndpoint.shutdown();
    }

    @Test
    void testHandleCourtelCallReturnsOk() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader("Content-Type",
                                                               ContentType.APPLICATION_JSON)
                                                    .setBody(PAYLOAD)
                                                    .setResponseCode(200));
        String response = thirdPartyService.handleCourtelCall(API, PAYLOAD);
        assertEquals(String.format("Successfully sent list to Courtel at: %s", API), response,
                     "Returned messages should match");
    }

    @Test
    void testHandleCourtelCallReturnsFailed() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ThirdPartyServiceException ex = assertThrows(ThirdPartyServiceException.class, () ->
            thirdPartyService.handleCourtelCall(API, PAYLOAD), "Should throw ThirdPartyException");
        assertTrue(ex.getMessage().contains(String.format("Third party request to: %s failed", API)),
                   "Messages should match");
    }

    @Test
    void testHandleCourtelCallReturnsOkAfterRetry() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .setResponseCode(200)
                                                    .addHeader("Content-Type", ContentType.APPLICATION_JSON)
                                                    .setBody(PAYLOAD));

        String response = thirdPartyService.handleCourtelCall(API, PAYLOAD);
        assertEquals(String.format("Successfully sent list to Courtel at: %s", API), response,
                     "Returned messages should match");
    }
}
