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
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocalSubscriptionMiData;
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
class SubscriptionManagementServiceTest extends RedisConfigurationTestBase {
    private static final String NOT_FOUND = "404";
    private static final String EXCEPTION_NOT_MATCH = "Exception does not match";
    private static final String MESSAGE_NOT_MATCH = "Message does not match";

    private static MockWebServer mockSubscriptionManagementEndpoint;

    @Autowired
    SubscriptionManagementService subscriptionManagementService;

    @BeforeEach
    void setup() throws IOException {
        mockSubscriptionManagementEndpoint = new MockWebServer();
        mockSubscriptionManagementEndpoint.start(8081);
    }

    @AfterEach
    void after() throws IOException {
        mockSubscriptionManagementEndpoint.close();
    }

    @Test
    void testGetAllMiDataReturnsOk() {
        subscriptionManagementService = mock(SubscriptionManagementService.class);

        AllSubscriptionMiData data1 = new AllSubscriptionMiData();
        List<AllSubscriptionMiData> expectedData = List.of(data1);

        when(subscriptionManagementService.getAllMiData()).thenReturn(expectedData);

        List<AllSubscriptionMiData> response = subscriptionManagementService.getAllMiData();

        assertEquals(expectedData, response, "Data do not match");
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
        subscriptionManagementService = mock(SubscriptionManagementService.class);

        LocalSubscriptionMiData data1 = new LocalSubscriptionMiData();
        List<LocalSubscriptionMiData> expectedData = List.of(data1);

        when(subscriptionManagementService.getLocationMiData()).thenReturn(expectedData);

        List<LocalSubscriptionMiData> response = subscriptionManagementService.getLocationMiData();

        assertEquals(expectedData, response, "Data do not match");
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
