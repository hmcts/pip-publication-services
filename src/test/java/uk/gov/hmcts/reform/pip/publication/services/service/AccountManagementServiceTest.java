package uk.gov.hmcts.reform.pip.publication.services.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.report.AccountMiData;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientTestConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {Application.class, WebClientTestConfiguration.class})
@DirtiesContext
@ActiveProfiles("test")
class AccountManagementServiceTest extends RedisConfigurationTestBase {
    private static final String NOT_FOUND = "404";
    private static MockWebServer mockAccountManagementEndpoint;

    @Autowired
    AccountManagementService accountManagementService;

    @BeforeEach
    void setup() throws IOException {
        mockAccountManagementEndpoint = new MockWebServer();
        mockAccountManagementEndpoint.start(8081);
    }

    @AfterEach
    void after() throws IOException {
        mockAccountManagementEndpoint.close();
    }

    @Test
    void testGetMiDataReturnsOk() {
        accountManagementService = mock(AccountManagementService.class);

        AccountMiData data1 = new AccountMiData();
        List<AccountMiData> expectedData = List.of(data1);

        when(accountManagementService.getMiData()).thenReturn(expectedData);

        List<AccountMiData> response = accountManagementService.getMiData();

        assertEquals(expectedData, response, "Data do not match");
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
