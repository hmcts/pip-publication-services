package uk.gov.hmcts.reform.pip.publication.services.service.thirdparty;

import nl.altindag.log.LogCaptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyOauthConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.models.ThirdPartyPublicationMetadata;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ActiveProfiles("test")
class ThirdPartyApiServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PUBLICATION_ID = UUID.randomUUID();
    private static final String DESTINATION_URL = "testUrl";
    private static final String PAYLOAD = "{}";
    private static final byte[] FILE = {1, 2, 3};
    private static final String FILENAME = "testFile.txt";

    private static final String ERROR_LOG_EMPTY_MESSAGE = "Error log should be empty";
    private static final String ERROR_LOG_NOT_EMPTY_MESSAGE = "Error log should not be empty";
    private static final String ERROR_LOG_MESSAGE = "Error log message does not match";

    private static final ThirdPartyOauthConfiguration OAUTH_CONFIGURATION = new ThirdPartyOauthConfiguration();
    private static final ThirdPartyPublicationMetadata METADATA = new ThirdPartyPublicationMetadata();

    private MockWebServer mockEndpoint = new MockWebServer();

    private ThirdPartyApiService thirdPartyApiService;

    @BeforeAll
    static void setUp() {
        OAUTH_CONFIGURATION.setUserId(USER_ID);
        OAUTH_CONFIGURATION.setDestinationUrl(DESTINATION_URL);
    }

    @BeforeEach
    void setUpEach() {
        WebClient mockedWebClient = WebClient.builder()
            .baseUrl(mockEndpoint.url(DESTINATION_URL).toString())
            .build();
        thirdPartyApiService = new ThirdPartyApiService(mockedWebClient, mock(ThirdPartyOauthService.class));

        ReflectionTestUtils.setField(thirdPartyApiService, "numOfRetries", 3);
        ReflectionTestUtils.setField(thirdPartyApiService, "backoff", 2);
    }

    @AfterEach
    void tearDownEach() throws IOException {
        mockEndpoint.close();
    }

    @Test
    void testSendNewPublicationToThirdPartySuccess() {
        try (LogCaptor logCaptor = LogCaptor.forClass(ThirdPartyApiService.class)) {
            mockEndpoint.enqueue(new MockResponse().setResponseCode(200));

            thirdPartyApiService.sendNewPublicationToThirdParty(OAUTH_CONFIGURATION, METADATA, PAYLOAD, FILE, FILENAME);

            assertThat(logCaptor.getErrorLogs())
                .as(ERROR_LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @Test
    void testSendNewPublicationToThirdPartySuccessAfterRetry() {
        try (LogCaptor logCaptor = LogCaptor.forClass(ThirdPartyApiService.class)) {
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));
            mockEndpoint.enqueue(new MockResponse().setResponseCode(200));

            thirdPartyApiService.sendNewPublicationToThirdParty(OAUTH_CONFIGURATION, METADATA, PAYLOAD, FILE, FILENAME);

            assertThat(logCaptor.getErrorLogs())
                .as(ERROR_LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @Test
    void testSendNewPublicationToThirdPartyError() {
        try (LogCaptor logCaptor = LogCaptor.forClass(ThirdPartyApiService.class)) {
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));

            thirdPartyApiService.sendNewPublicationToThirdParty(OAUTH_CONFIGURATION, METADATA, PAYLOAD, FILE, FILENAME);

            assertThat(logCaptor.getErrorLogs())
                .as(ERROR_LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().get(0))
                .as(ERROR_LOG_MESSAGE)
                .contains("Failed to send new publication to third party user with ID " + USER_ID);
        }
    }

    @Test
    void testSendUpdatedPublicationToThirdPartySuccess() {
        try (LogCaptor logCaptor = LogCaptor.forClass(ThirdPartyApiService.class)) {
            mockEndpoint.enqueue(new MockResponse().setResponseCode(200));

            thirdPartyApiService.sendUpdatedPublicationToThirdParty(OAUTH_CONFIGURATION, METADATA, PAYLOAD, FILE,
                                                                    FILENAME);

            assertThat(logCaptor.getErrorLogs())
                .as(ERROR_LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @Test
    void testSendUpdatedPublicationToThirdPartySuccessAfterRetry() {
        try (LogCaptor logCaptor = LogCaptor.forClass(ThirdPartyApiService.class)) {
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));
            mockEndpoint.enqueue(new MockResponse().setResponseCode(200));

            thirdPartyApiService.sendUpdatedPublicationToThirdParty(OAUTH_CONFIGURATION, METADATA, PAYLOAD, FILE,
                                                                    FILENAME);

            assertThat(logCaptor.getErrorLogs())
                .as(ERROR_LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @Test
    void testSendUpdatedPublicationToThirdPartyError() {
        try (LogCaptor logCaptor = LogCaptor.forClass(ThirdPartyApiService.class)) {
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));

            thirdPartyApiService.sendUpdatedPublicationToThirdParty(OAUTH_CONFIGURATION, METADATA, PAYLOAD, FILE,
                                                                    FILENAME);

            assertThat(logCaptor.getErrorLogs())
                .as(ERROR_LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().get(0))
                .as(ERROR_LOG_MESSAGE)
                .contains("Failed to send updated publication to third party user with ID " + USER_ID);
        }
    }

    @Test
    void testNotifyThirdPartyOfPublicationDeletionSuccess() {
        try (LogCaptor logCaptor = LogCaptor.forClass(ThirdPartyApiService.class)) {
            mockEndpoint.enqueue(new MockResponse().setResponseCode(200));

            thirdPartyApiService.notifyThirdPartyOfPublicationDeletion(OAUTH_CONFIGURATION, PUBLICATION_ID);

            assertThat(logCaptor.getErrorLogs())
                .as(ERROR_LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @Test
    void testNotifyThirdPartyOfPublicationDeletionSuccessAfterRetry() {
        try (LogCaptor logCaptor = LogCaptor.forClass(ThirdPartyApiService.class)) {
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));
            mockEndpoint.enqueue(new MockResponse().setResponseCode(200));

            thirdPartyApiService.notifyThirdPartyOfPublicationDeletion(OAUTH_CONFIGURATION, PUBLICATION_ID);

            assertThat(logCaptor.getErrorLogs())
                .as(ERROR_LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @Test
    void testNotifyThirdPartyOfPublicationDeletionError() {
        try (LogCaptor logCaptor = LogCaptor.forClass(ThirdPartyApiService.class)) {
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));

            thirdPartyApiService.notifyThirdPartyOfPublicationDeletion(OAUTH_CONFIGURATION, PUBLICATION_ID);

            assertThat(logCaptor.getErrorLogs())
                .as(ERROR_LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().get(0))
                .as(ERROR_LOG_MESSAGE)
                .contains("Failed to send publication deleted notification to third party user with ID " + USER_ID);
        }
    }
}
