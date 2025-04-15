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
import uk.gov.hmcts.reform.pip.model.report.AccountMiData;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
class AccountManagementServiceTest {
    private static final String NOT_FOUND = "404";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    void testGetAccountMiDataReturnsOk() throws JsonProcessingException {
        AccountMiData data1 = new AccountMiData();
        List<AccountMiData> expectedData = List.of(data1);

        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                                  .addHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON)
                                                  .setBody(OBJECT_MAPPER.writeValueAsString(expectedData))
                                                  .setResponseCode(200));

        List<AccountMiData> response = accountManagementService.getAccountMiData();
        assertEquals(expectedData, response, RESPONSE_NOT_MATCH);
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
    void testGetAllMiDataReturnsOk() throws JsonProcessingException {
        AllSubscriptionMiData data1 = new AllSubscriptionMiData();
        List<AllSubscriptionMiData> expectedData = List.of(data1);

        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                                  .addHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON)
                                                  .setBody(OBJECT_MAPPER.writeValueAsString(expectedData))
                                                  .setResponseCode(200));

        List<AllSubscriptionMiData> response = accountManagementService.getAllSubscriptionMiData();

        assertEquals(expectedData, response, "Data does not match");
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
    void testGetLocationMiDataReturnsOk() throws JsonProcessingException {
        LocationSubscriptionMiData data1 = new LocationSubscriptionMiData();
        List<LocationSubscriptionMiData> expectedData = List.of(data1);

        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                                  .addHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON)
                                                  .setBody(OBJECT_MAPPER.writeValueAsString(expectedData))
                                                  .setResponseCode(200));

        List<LocationSubscriptionMiData> response = accountManagementService.getLocationSubscriptionMiData();
        assertEquals(expectedData, response, "Data does not match");
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
