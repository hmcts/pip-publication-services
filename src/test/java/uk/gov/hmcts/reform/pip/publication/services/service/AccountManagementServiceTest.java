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
    void testGetMiDataReturnsOk() throws JsonProcessingException {
        AccountMiData data1 = new AccountMiData();
        List<AccountMiData> expectedData = List.of(data1);

        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                                  .addHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON)
                                                  .setBody(OBJECT_MAPPER.writeValueAsString(expectedData))
                                                  .setResponseCode(200));

        List<AccountMiData> response = accountManagementService.getMiData();

        assertEquals(expectedData, response, "Data does not match");
    }

    @Test
    void testGetMiDataThrowsException() {
        mockAccountManagementEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ServiceToServiceException notifyException = assertThrows(ServiceToServiceException.class,
                                                                 accountManagementService::getMiData,
                                                                 "Exception does not match");

        assertTrue(notifyException.getMessage().contains(NOT_FOUND), "Message does not match");
    }
}
