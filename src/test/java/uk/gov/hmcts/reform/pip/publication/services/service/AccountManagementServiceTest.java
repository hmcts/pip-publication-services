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
class AccountManagementServiceTest {
    private static final String RESPONSE_BODY = "responseBody";
    private static final String NOT_FOUND = "404";
    private static final String RESPONSE_NOT_MATCH = "Response does not match";
    private static final String EXCEPTION_NOT_MATCH = "Exception does not match";
    private static final String MESSAGE_NOT_MATCH = "Message does not match";

    private final MockWebServer mockAccountManagementEndpoint = new MockWebServer();

    private AccountManagementService accountManagementService;

    @BeforeEach
    void setup() {
        WebClient mockedWebClient = WebClient.builder()
            .baseUrl(mockAccountManagementEndpoint.url("/").toString())
            .build();
        accountManagementService = new AccountManagementService(mockedWebClient);
    }

    @AfterEach
    void after() throws IOException {
        mockAccountManagementEndpoint.close();
    }

    @Test
    void testGetAccountMiDataReturnsOk() {
        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                                    .setBody(RESPONSE_BODY)
                                                    .setResponseCode(200));

        String response = accountManagementService.getAccountMiData();
        assertEquals(RESPONSE_BODY, response, RESPONSE_NOT_MATCH);
    }

    @Test
    void testGetMiDataThrowsException() {
        mockAccountManagementEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class,
                                                                 accountManagementService::getAccountMiData,
                                                                 EXCEPTION_NOT_MATCH);

        assertTrue(notifyException.getMessage().contains(NOT_FOUND), "Message does not match");
    }

    @Test
    void testGetAllMiDataReturnsOk() {
        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                                  .setBody(RESPONSE_BODY)
                                                  .setResponseCode(200));

        String response = accountManagementService.getAllSubscriptionMiData();
        assertEquals(RESPONSE_BODY, response, RESPONSE_NOT_MATCH);
    }

    @Test
    void testGetAllMiDataThrowsException() {
        mockAccountManagementEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class,
                                                                 accountManagementService::getAllSubscriptionMiData,
                                                                 EXCEPTION_NOT_MATCH);

        assertTrue(notifyException.getMessage().contains(NOT_FOUND), MESSAGE_NOT_MATCH);
    }

    @Test
    void testGetLocationMiDataReturnsOk() {
        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                                  .setBody(RESPONSE_BODY)
                                                  .setResponseCode(200));

        String response = accountManagementService.getLocationSubscriptionMiData();
        assertEquals(RESPONSE_BODY, response, RESPONSE_NOT_MATCH);
    }

    @Test
    void testGetLocationMiDataThrowsException() {
        mockAccountManagementEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ServiceToServiceException notifyException = assertThrows(
            ServiceToServiceException.class, accountManagementService::getLocationSubscriptionMiData,
            EXCEPTION_NOT_MATCH
        );

        assertTrue(notifyException.getMessage().contains(NOT_FOUND), MESSAGE_NOT_MATCH);
    }
}
