package uk.gov.hmcts.reform.pip.publication.services.service.thirdparty;

import nl.altindag.log.LogCaptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyOauthConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.service.KeyVaultService;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
class ThirdPartyOauthServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TOKEN_URL = "testUrl";
    private static final String ACCESS_TOKEN = "testToken";

    private static final String ERROR_LOG_EMPTY_MESSAGE = "Error log should be empty";
    private static final String ERROR_LOG_NOT_EMPTY_MESSAGE = "Error log should not be empty";
    private static final String ERROR_LOG_MESSAGE = "Error log message does not match";

    private static final ThirdPartyOauthConfiguration OAUTH_CONFIGURATION = new ThirdPartyOauthConfiguration();

    private ThirdPartyTokenCachingService thirdPartyTokenCachingService = mock(ThirdPartyTokenCachingService.class);
    private KeyVaultService keyVaultService = mock(KeyVaultService.class);
    private MockWebServer mockEndpoint = new MockWebServer();

    private ThirdPartyOauthService thirdPartyOauthService;

    @BeforeAll
    static void setUp() {
        OAUTH_CONFIGURATION.setUserId(USER_ID);
        OAUTH_CONFIGURATION.setTokenUrl(TOKEN_URL);
    }

    @BeforeEach
    void setUpEach() {
        WebClient mockedWebClient = WebClient.builder()
            .baseUrl(mockEndpoint.url(TOKEN_URL).toString())
            .build();
        thirdPartyOauthService = new ThirdPartyOauthService(mockedWebClient, keyVaultService,
                                                            thirdPartyTokenCachingService);
    }

    @AfterEach
    void tearDownEach() throws IOException {
        mockEndpoint.close();
    }

    @Test
    void testGetApiAccessTokenWithNoCachedToken() {
        when(thirdPartyTokenCachingService.getCachedToken(any())).thenReturn(null);
        try (LogCaptor logCaptor = LogCaptor.forClass(ThirdPartyOauthService.class)) {
            mockEndpoint.enqueue(new MockResponse().setResponseCode(200));

            thirdPartyOauthService.getApiAccessToken(OAUTH_CONFIGURATION);

            assertThat(logCaptor.getErrorLogs())
                .as(ERROR_LOG_EMPTY_MESSAGE)
                .isEmpty();

            //verify(keyVaultService).getSecretValue(any());
        }
    }

    @Test
    void testGetApiAccessTokenUsingCachedToken() {
        when(thirdPartyTokenCachingService.getCachedToken(any())).thenReturn(ACCESS_TOKEN);
        try (LogCaptor logCaptor = LogCaptor.forClass(ThirdPartyOauthService.class)) {

            thirdPartyOauthService.getApiAccessToken(OAUTH_CONFIGURATION);

            assertThat(logCaptor.getErrorLogs())
                .as(ERROR_LOG_EMPTY_MESSAGE)
                .isEmpty();

            verifyNoInteractions(keyVaultService);
        }
    }

    @Test
    void testGetApiAccessTokenError() {
        when(thirdPartyTokenCachingService.getCachedToken(any())).thenReturn(null);
        try (LogCaptor logCaptor = LogCaptor.forClass(ThirdPartyOauthService.class)) {
            mockEndpoint.enqueue(new MockResponse().setResponseCode(404));

            thirdPartyOauthService.getApiAccessToken(OAUTH_CONFIGURATION);

            assertThat(logCaptor.getErrorLogs())
                .as(ERROR_LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().get(0))
                .as(ERROR_LOG_MESSAGE)
                .contains("Failed to generate access token for third party user with ID " + USER_ID);
        }
    }
}
