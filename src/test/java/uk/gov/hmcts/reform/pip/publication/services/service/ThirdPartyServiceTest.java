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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        mockPublicationServicesEndpoint.close();
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

        String response = thirdPartyService.handleCourtelCall(API, PAYLOAD);
        assertEquals("Request Failed", response, "Returned messages should match");
    }

    //TODO: write the fail test and then some bloody int tests then you need to go into channel mangement and write
    // the thing that gets the api, might need to change file to octet stream content type
}
